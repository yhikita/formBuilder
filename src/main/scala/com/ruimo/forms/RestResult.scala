package com.ruimo.forms

import play.api.libs.json.JsValue
import scala.collection.{immutable => imm}
import scalafx.scene.image.Image

sealed trait RetrievePreparedImageRestResult
sealed trait RetrievePreparedImageRestFailure extends RetrievePreparedImageRestResult

sealed trait CaptureRestResult
sealed trait CaptureRestFailure extends CaptureRestResult

sealed trait SaveConfigRestResult
sealed trait SaveConfigRestFailure extends SaveConfigRestResult

case class RestAuthFailure(
  statusCode: Int,
  statusText: String,
  body: String
) extends RetrievePreparedImageRestFailure with CaptureRestFailure with SaveConfigRestFailure

case class RestUnknownFailure(
  statusCode: Int,
  statusText: String,
  body: String
) extends RetrievePreparedImageRestFailure with CaptureRestFailure with SaveConfigRestFailure

class RetrievePreparedImageResultOk(
  val serverResp: Option[PrepareResult],
  val image: Image
) extends RetrievePreparedImageRestResult

class CaptureResultOk(
  val serverResp: imm.Seq[CaptureResult]
) extends CaptureRestResult

object CaptureResultOk {
  def parse(json: JsValue): CaptureResultOk = {
    val capturedFields = (json \ "capturedFields").as[Seq[JsValue]]
    new CaptureResultOk(
      capturedFields.map { e =>
        CaptureResult(
          (e \ "fieldName").as[String],
          (e \ "base64Image").as[String],
          (e \ "text").as[String],
          (e \ "rawText").as[String]
        )
      }.toList
    )
  }
}

case class CaptureResult(
  fieldName: String, base64Image: String, text: String, rawText: String
)

case class Revision(value: Long) extends AnyVal

case class SaveConfigResult(
  revision: Revision
)

case class SaveConfigResultOk(
  serverResp: SaveConfigResult
) extends SaveConfigRestResult

object SaveConfigResultOk {
  def parse(json: JsValue): SaveConfigResponse = SaveConfigResponse(
    SaveConfigResult(
      Revision((json \ "revision").as[String].toLong)
    )
  )
}
