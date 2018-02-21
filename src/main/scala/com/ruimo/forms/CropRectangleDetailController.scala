package com.ruimo.forms

import javafx.fxml.{FXML, Initializable}
import java.net.URL
import java.util.ResourceBundle
import javafx.scene.control.{ComboBox, TextField}

import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ComboBox => SfxComboBox}

sealed trait CropRectangleDetailValidation
case object InvalidErrorAllowance extends CropRectangleDetailValidation
case object InvalidTopMargin extends CropRectangleDetailValidation
case object InvalidLeftMargin extends CropRectangleDetailValidation
case object InvalidRightMargin extends CropRectangleDetailValidation
case object InvalidBottomMargin extends CropRectangleDetailValidation

class CropRectangleDetailController extends Initializable {
  @FXML
  private[this] var errorAllowanceText: TextField = _

  def errorAllowance_=(ea: Int) {
    errorAllowanceText.setText(ea.toString)
  }

  def errorAllowance: Int = errorAllowanceText.getText.toInt

  @FXML
  private[this] var topMarginText: TextField = _

  def topMargin_=(ea: Double) {
    topMarginText.setText(ea.toString)
  }

  def topMargin: Double = topMarginText.getText.toDouble

  @FXML
  private[this] var leftMarginText: TextField = _

  def leftMargin_=(ea: Double) {
    leftMarginText.setText(ea.toString)
  }

  def leftMargin: Double = leftMarginText.getText.toDouble

  @FXML
  private[this] var rightMarginText: TextField = _

  def rightMargin_=(ea: Double) {
    rightMarginText.setText(ea.toString)
  }

  def rightMargin: Double = rightMarginText.getText.toDouble

  @FXML
  private[this] var bottomMarginText: TextField = _

  def bottomMargin_=(ea: Double) {
    bottomMarginText.setText(ea.toString)
  }

  def bottomMargin: Double = bottomMarginText.getText.toDouble

  def validate: Option[CropRectangleDetailValidation] = {
    (try {
      val value = errorAllowance
      if (value < 0) Some(InvalidErrorAllowance) else None
    }
    catch {
      case e: NumberFormatException =>
        Some(InvalidErrorAllowance)
    }).orElse {
      try {
        val value = topMargin
        if (value < 0.0) Some(InvalidTopMargin) else None
      }
      catch {
        case e: NumberFormatException =>
          Some(InvalidTopMargin)
      }
    }.orElse {
      try {
        val value = leftMargin
        if (value < 0.0) Some(InvalidLeftMargin) else None
      }
      catch {
        case e: NumberFormatException =>
          Some(InvalidLeftMargin)
      }
    }.orElse {
      try {
        val value = rightMargin
        if (value < 0.0) Some(InvalidRightMargin) else None
      }
      catch {
        case e: NumberFormatException =>
          Some(InvalidRightMargin)
      }
    }.orElse {
      try {
        val value = bottomMargin
        if (value < 0.0) Some(InvalidBottomMargin) else None
      }
      catch {
        case e: NumberFormatException =>
          Some(InvalidBottomMargin)
      }
    }
  }

  def model: CropRectangleCondition = CropRectangleConditionImpl(
    errorAllowance = errorAllowance,
    topMargin = topMargin,
    leftMargin = leftMargin,
    rightMargin = rightMargin,
    bottomMargin = bottomMargin
  )

  def model_=(newModel: CropRectangleCondition) {
    errorAllowance = newModel.errorAllowance
    topMargin = newModel.topMargin
    leftMargin = newModel.leftMargin
    rightMargin = newModel.rightMargin
    bottomMargin = newModel.bottomMargin
  }

  override def initialize(url: URL, resourceBundle: ResourceBundle) {
    println("CropRectangleDetailController initialize")
  }
}