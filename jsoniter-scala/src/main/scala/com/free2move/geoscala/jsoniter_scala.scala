/*
 * Copyright 2019 GHM Mobile Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.free2move.geoscala

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import scala.annotation.nowarn

object jsoniter_scala {
  // Uncomment for printing of generating codecs
  // implicit val printCodec: CodecMakerConfig.PrintCodec = new CodecMakerConfig.PrintCodec {}

  implicit val coordinateCodec: JsonValueCodec[Coordinate] =
    new JsonValueCodec[Coordinate] {
      override def decodeValue(in: JsonReader, default: Coordinate): Coordinate =
        if (in.isNextToken('[')) {
          val lon = in.readDouble()
          if (!in.isNextToken(',')) in.commaError()
          val lat = in.readDouble()
          while (in.isNextToken(',')) in.skip()
          if (!in.isCurrentToken(']')) in.arrayEndOrCommaError()
          Coordinate(lon, lat)
        } else in.readNullOrTokenError(default, '[')

      override def encodeValue(x: Coordinate, out: JsonWriter): Unit = {
        out.writeArrayStart()
        out.writeVal(x.longitude)
        out.writeVal(x.latitude)
        out.writeArrayEnd()
      }

      override def nullValue: Coordinate = null
    }

  implicit val listOfCoordinatesCodec: JsonValueCodec[List[Coordinate]] = JsonCodecMaker.make

  implicit val listOfListOfCoordinatesCodec: JsonValueCodec[List[List[Coordinate]]] = JsonCodecMaker.make

  implicit val listOfListOfListOfCoordinatesCodec: JsonValueCodec[List[List[List[Coordinate]]]] = JsonCodecMaker.make

  implicit val pointCodec: JsonValueCodec[Point] =
    makeGeometryCodec("Point", _.coordinates, Point.apply)

  implicit val multiPointCodec: JsonValueCodec[MultiPoint] =
    makeGeometryCodec("MultiPoint", _.coordinates, MultiPoint.apply)

  implicit val lineStringCodec: JsonValueCodec[LineString] =
    makeGeometryCodec("LineString", _.coordinates, LineString.apply)

  implicit val multiLineStringCodec: JsonValueCodec[MultiLineString] =
    makeGeometryCodec("MultiLineString", _.coordinates, MultiLineString.apply)

  implicit val polygonCodec: JsonValueCodec[Polygon] =
    makeGeometryCodec("Polygon", _.coordinates, Polygon.apply)

  implicit val multiPolygonCodec: JsonValueCodec[MultiPolygon] =
    makeGeometryCodec("MultiPolygon", _.coordinates, MultiPolygon.apply)

  private def makeGeometryCodec[C: JsonValueCodec, G <: Geometry](`type`: String, coords: G => C, geom: C => G): JsonValueCodec[G] =
    new JsonValueCodec[G] {
      private val coordinatesCodec: JsonValueCodec[C] = implicitly[JsonValueCodec[C]]

      override def decodeValue(in: JsonReader, default: G): G =
        if (in.isNextToken('{')) {
          var coordinates: C = coordinatesCodec.nullValue
          var mask = 3
          var len = -1
          while (len < 0 || in.isNextToken(',')) {
            len = in.readKeyAsCharBuf()
            if (in.isCharBufEqualsTo(len, "type")) {
              if ((mask & 0x1) != 0) mask ^= 0x1
              else in.duplicatedKeyError(len)
              len = in.readStringAsCharBuf()
              if (!in.isCharBufEqualsTo(len, `type`)) in.discriminatorValueError("type")
            } else if (in.isCharBufEqualsTo(len, "coordinates")) {
              if ((mask & 0x2) != 0) mask ^= 0x2
              else in.duplicatedKeyError(len)
              coordinates = coordinatesCodec.decodeValue(in, coordinates)
            } else in.skip()
          }
          geom(coordinates)
        } else in.readNullOrTokenError(default, '}')

      override def encodeValue(x: G, out: JsonWriter): Unit = {
        out.writeObjectStart()
        out.writeNonEscapedAsciiKey("type")
        out.writeNonEscapedAsciiVal(`type`)
        out.writeNonEscapedAsciiKey("coordinates")
        coordinatesCodec.encodeValue(coords(x), out)
        out.writeObjectEnd()
      }

      override def nullValue: G = null.asInstanceOf[G]
    }

  implicit val geometryCodec: JsonValueCodec[Geometry] = JsonCodecMaker.make

  @nowarn
  implicit def featureCodec[P: JsonValueCodec]: JsonValueCodec[Feature[P]] = JsonCodecMaker.make

  @nowarn
  implicit def featureCollectionCodec[P: JsonValueCodec]: JsonValueCodec[FeatureCollection[P]] = JsonCodecMaker.make

  implicit def geoJson[P: JsonValueCodec]: JsonValueCodec[GeoJson[P]] =
    new JsonValueCodec[GeoJson[P]] {
      private val fc: JsonValueCodec[Feature[P]] = featureCodec
      private val fcc: JsonValueCodec[FeatureCollection[P]] = featureCollectionCodec

      override def decodeValue(in: JsonReader, default: GeoJson[P]): GeoJson[P] = {
        in.setMark()
        if (in.isNextToken('{')) {
          if (!in.skipToKey("type")) in.discriminatorError()
          val len = in.readStringAsCharBuf()
          in.rollbackToMark()
          if (in.isCharBufEqualsTo(len, "Feature")) fc.decodeValue(in, fc.nullValue)
          else if (in.isCharBufEqualsTo(len, "FeatureCollection")) fcc.decodeValue(in, fcc.nullValue)
          else geometryCodec.decodeValue(in, geometryCodec.nullValue).asInstanceOf[GeoJson[P]]
        } else {
          val gj = in.readNullOrTokenError(default, '{')
          in.rollbackToMark()
          in.skip()
          gj
        }
      }

      override def encodeValue(x: GeoJson[P], out: JsonWriter): Unit =
        x match {
          case f: Feature[P]            => fc.encodeValue(f, out)
          case fc: FeatureCollection[P] => fcc.encodeValue(fc, out)
          case _                        => geometryCodec.encodeValue(x.asInstanceOf[Geometry], out)
        }

      override def nullValue: GeoJson[P] = null.asInstanceOf[GeoJson[P]]
    }
}
