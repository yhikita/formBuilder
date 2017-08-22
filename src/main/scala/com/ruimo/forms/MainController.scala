package com.ruimo.forms

import java.net.URL
import java.io._

import scalafx.scene.canvas.{GraphicsContext => SfxGraphicsContext}
import scala.collection.{immutable => imm, mutable => mut}
import javafx.fxml.{FXML, FXMLLoader, Initializable}
import javafx.event.ActionEvent
import javafx.scene.input.{KeyCode, KeyEvent, MouseEvent}
import javafx.event.EventHandler

import scalafx.collections.ObservableBuffer
import scalafx.application.Platform
import scalafx.scene.image.Image
import scalafx.stage.{FileChooser, Modality, Stage, StageStyle}
import java.net.{HttpURLConnection, URL}
import java.nio.file.{Files, Path, Paths}
import java.util.ResourceBundle
import java.util.zip.{ZipEntry, ZipOutputStream}
import javafx.scene.{Cursor, Scene}

import scalafx.scene.control.{Alert => SfxAlert, ListView => SfxListView, TextInputDialog => SfxTextInputDialog}
import scalafx.scene.control.{ChoiceDialog => SfxChoiceDialog}
import scalafx.scene.control.{CheckBox => SfxCheckBox}
import javafx.scene.canvas.Canvas
import javafx.scene.control._
import javafx.scene.paint.Color

import scalafx.scene.canvas.{Canvas => SfxCanvas}
import scalafx.scene.control.{DialogPane => SfxDialogPane}
import scalafx.scene.control.Alert.AlertType
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ruimo.scoins.LoanPattern
import com.ruimo.scoins.LoanPattern._
import play.api.libs.json.{JsObject, Json}

import scalafx.geometry.{Point2D, Rectangle2D}
import Helpers.toRect
import org.w3c.dom.events.EventListener

case class EditorContext(
  drawWidget: (Widget[_], Boolean) => Unit,
  redrawRect: (Rectangle2D) => Unit,
  fieldCreated: (Field) => Unit,
  drawSelectionRect: (Rectangle2D) => Unit,
  selectFields: (Rectangle2D, MouseEvent) => Unit
)

object ModeEditors {
  sealed trait ModeEditor {
    def onMousePressed(e: MouseEvent): ModeEditor
    def onMouseDragged(e: MouseEvent): ModeEditor
    def onMouseReleased(e: MouseEvent): ModeEditor
    def switchToAddMode(e: ActionEvent): ModeEditor
    def switchToAddCropMode(e: ActionEvent): ModeEditor
    def switchToSelectMode(e: ActionEvent): ModeEditor
  }

  object AddCropMode {
    case class Init(
      project: Project,
      editorContext: EditorContext
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = {
        project.deselectAllFields()
        Start(project, editorContext, new Point2D(e.getX, e.getY))
      }
      def onMouseDragged(e: MouseEvent): ModeEditor = this
      def onMouseReleased(e: MouseEvent): ModeEditor = this
      def switchToAddMode(e: ActionEvent): ModeEditor = this
      def switchToAddCropMode(e: ActionEvent): ModeEditor = this
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }

    case class Start(
      project: Project,
      editorContext: EditorContext,
      p0: Point2D
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = this
      def onMouseDragged(e: MouseEvent): ModeEditor = {
        val p1 = new Point2D(e.getX, e.getY)
        val adding = new CropFieldImpl(toRect(p0, p1))
        editorContext.drawWidget(adding, true)
        Dragging(adding, project, editorContext, p0, p1)
      }
      def onMouseReleased(e: MouseEvent): ModeEditor = Init(project, editorContext)
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }

    case class Dragging(
      adding: Field,
      project: Project,
      editorContext: EditorContext,
      p0: Point2D, p1: Point2D
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = this
      def onMouseDragged(e: MouseEvent): ModeEditor = {
        val newP1 = new Point2D(e.getX, e.getY)
        editorContext.redrawRect(adding.drawArea)
        val newAdding = new CropFieldImpl(toRect(p0, newP1))
        editorContext.drawWidget(newAdding, true)
        Dragging(newAdding, project, editorContext, p0, newP1)
      }
      def onMouseReleased(e: MouseEvent): ModeEditor = {
        val newP1 = new Point2D(e.getX, e.getY)
        editorContext.redrawRect(adding.drawArea)
        val newAdding = new CropFieldImpl(toRect(p0, newP1))
        editorContext.drawWidget(newAdding, true)
        editorContext.fieldCreated(newAdding)
        Init(project, editorContext)
      }
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }
  }

  object AddMode {
    case class Init(
      project: Project,
      editorContext: EditorContext
    ) extends ModeEditor {
      assume(project != null)
      assume(editorContext != null)

      def onMousePressed(e: MouseEvent): ModeEditor = {
        project.deselectAllFields()
        Start(project, editorContext, new Point2D(e.getX, e.getY))
      }
      def onMouseDragged(e: MouseEvent): ModeEditor = this
      def onMouseReleased(e: MouseEvent): ModeEditor = this
      def switchToAddMode(e: ActionEvent): ModeEditor = this
      def switchToAddCropMode(e: ActionEvent): ModeEditor = AddCropMode.Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }

    case class Start(
      project: Project,
      editorContext: EditorContext,
      p0: Point2D
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = this
      def onMouseDragged(e: MouseEvent): ModeEditor = {
        val p1 = new Point2D(e.getX, e.getY)
        val adding = AbsoluteFieldImpl(toRect(p0, p1), "adding")
        editorContext.drawWidget(adding, true)
        Dragging(adding, project, editorContext, p0, p1)
      }
      def onMouseReleased(e: MouseEvent): ModeEditor = Init(project, editorContext)
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = AddCropMode.Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }

    case class Dragging(
      adding: Field,
      project: Project,
      editorContext: EditorContext,
      p0: Point2D, p1: Point2D
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = this
      def onMouseDragged(e: MouseEvent): ModeEditor = {
        val newP1 = new Point2D(e.getX, e.getY)
        editorContext.redrawRect(adding.drawArea)
        val newAdding = AbsoluteFieldImpl(toRect(p0, newP1), "adding")
        editorContext.drawWidget(newAdding, true)
        Dragging(newAdding, project, editorContext, p0, newP1)
      }
      def onMouseReleased(e: MouseEvent): ModeEditor = {
        val newP1 = new Point2D(e.getX, e.getY)
        editorContext.redrawRect(adding.drawArea)
        val newAdding = AbsoluteFieldImpl(toRect(p0, newP1), "adding")
        editorContext.drawWidget(newAdding, true)
        editorContext.fieldCreated(newAdding)
        Init(project, editorContext)
      }
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = AddCropMode.Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }
  }

  object SelectMode {
    case class Init(
      project: Project,
      editorContext: EditorContext
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = {
        def doNext(f: AbsoluteField): ModeEditor = {
          f.possibleMouseOperation(e.getX, e.getY) match {
            case CanMove(_) => Moving(project, editorContext, new Point2D(e.getX, e.getY))
            case CanNorthResize(f) => NorthResize(f, project, editorContext, new Point2D(e.getX, e.getY))
            case CanEastResize(_) => Moving(project, editorContext, new Point2D(e.getX, e.getY))
            case CanWestResize(_) => Moving(project, editorContext, new Point2D(e.getX, e.getY))
            case CanSouthResize(_) => Moving(project, editorContext, new Point2D(e.getX, e.getY))
            case CanNorthWestResize(_) => Moving(project, editorContext, new Point2D(e.getX, e.getY))
            case CanNorthEastResize(_) => Moving(project, editorContext, new Point2D(e.getX, e.getY))
            case CanSouthWestResize(_) => Moving(project, editorContext, new Point2D(e.getX, e.getY))
            case CanSouthEastResize(_) => Moving(project, editorContext, new Point2D(e.getX, e.getY))
            case CanDoNothing => Moving(project, editorContext, new Point2D(e.getX, e.getY))
          }
        }

        project.getSelectedAbsoluteField(e.getX, e.getY) match {
          case Some(f) => doNext(f)
          case None =>
            project.getNormalAbsoluteField(e.getX, e.getY) match {
              case Some(f) =>
                if (! e.isShiftDown)
                  project.deselectAllFields()
                project.selectAbsoluteField(f)
                doNext(f)
              case None =>
                if (project.leftCropField.map { _.drawArea.contains(e.getX, e.getY) }.getOrElse(false)) {
                  if (! e.isShiftDown)
                    project.deselectAllFields()
                  project.selectLeftCropField(true)
                  Moving(project, editorContext, new Point2D(e.getX, e.getY))
                }
                else if (project.topCropField.map { _.drawArea.contains(e.getX, e.getY) }.getOrElse(false)) {
                  if (! e.isShiftDown)
                    project.deselectAllFields()
                  project.selectTopCropField(true)
                  Moving(project, editorContext, new Point2D(e.getX, e.getY))
                }
                else if (project.rightCropField.map { _.drawArea.contains(e.getX, e.getY) }.getOrElse(false)) {
                  if (! e.isShiftDown)
                    project.deselectAllFields()
                  project.selectRightCropField(true)
                  Moving(project, editorContext, new Point2D(e.getX, e.getY))
                }
                else if (project.bottomCropField.map { _.drawArea.contains(e.getX, e.getY) }.getOrElse(false)) {
                  if (! e.isShiftDown)
                    project.deselectAllFields()
                  project.selectBottomCropField(true)
                  Moving(project, editorContext, new Point2D(e.getX, e.getY))
                }
                else {
                  if (! e.isShiftDown)
                    project.deselectAllFields()
                  Start(project, editorContext, new Point2D(e.getX, e.getY))
                }
            }
        }
      }
      def onMouseDragged(e: MouseEvent): ModeEditor = this
      def onMouseReleased(e: MouseEvent): ModeEditor = this
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = AddCropMode.Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = this
    }

    case class Start(
      project: Project,
      editorContext: EditorContext,
      p0: Point2D
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = this
      def onMouseDragged(e: MouseEvent): ModeEditor = {
        val p1 = new Point2D(e.getX, e.getY)
        val sw = SelectionWidget(toRect(p0, p1))
        editorContext.drawWidget(sw, false)
        Dragging(sw, project, editorContext, p0, p1)
      }
      def onMouseReleased(e: MouseEvent): ModeEditor = {
        project.selectSingleFieldAt(e.getX, e.getY)
        Init(project, editorContext)
      }
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = AddCropMode.Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }

    case class Moving(
      project: Project,
      editorContext: EditorContext,
      p0: Point2D
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = this
      def onMouseDragged(e: MouseEvent): ModeEditor = {
        val p1 = new Point2D(e.getX, e.getY)
        project.moveSelectedAbsoluteFields(p0, p1)
        Moving(project, editorContext, p1)
      }
      def onMouseReleased(e: MouseEvent): ModeEditor = Init(project, editorContext)
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = AddCropMode.Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }

    case class NorthResize(
      f: Field,
      project: Project,
      editorContext: EditorContext,
      p0: Point2D
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = this
      def onMouseDragged(e: MouseEvent): ModeEditor = {
        val p1 = new Point2D(e.getX, e.getY)
        project.northResize(f, p0, p1)
        NorthResize(f, project, editorContext, p0)
      }
      def onMouseReleased(e: MouseEvent): ModeEditor = Init(project, editorContext)
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = AddCropMode.Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }

    case class Dragging(
      sw: SelectionWidget,
      project: Project,
      editorContext: EditorContext,
      p0: Point2D, p1: Point2D
    ) extends ModeEditor {
      def onMousePressed(e: MouseEvent): ModeEditor = this
      def onMouseDragged(e: MouseEvent): ModeEditor = {
        val newP1 = new Point2D(e.getX, e.getY)
        val newSw = SelectionWidget(toRect(p0, newP1))

        editorContext.redrawRect(sw.drawArea)
        editorContext.drawWidget(newSw, false)
        Dragging(newSw, project, editorContext, p0, newP1)
      }
      def onMouseReleased(e: MouseEvent): ModeEditor = {
        val newP1 = new Point2D(e.getX, e.getY)
        editorContext.redrawRect(sw.drawArea)
        editorContext.selectFields(toRect(p0, newP1), e)
        Init(project, editorContext)
      }
      def switchToAddMode(e: ActionEvent): ModeEditor = AddMode.Init(project, editorContext)
      def switchToAddCropMode(e: ActionEvent): ModeEditor = AddCropMode.Init(project, editorContext)
      def switchToSelectMode(e: ActionEvent): ModeEditor = SelectMode.Init(project, editorContext)
    }
  }
}

class Editor(
  project: Project,
  editorContext: EditorContext
) {
  private var editor: ModeEditors.ModeEditor = ModeEditors.AddMode.Init(
    project, editorContext
  )

  def initialize() {
  }

  def onMousePressed(e: MouseEvent) {
    editor = editor.onMousePressed(e)
  }

  def onMouseDragged(e: MouseEvent): Unit = {
    editor = editor.onMouseDragged(e)
  }

  def onMouseReleased(e: MouseEvent): Unit = {
    editor = editor.onMouseReleased(e)
  }

  def switchToAddMode(e: ActionEvent): Unit = {
    editor = editor.switchToAddMode(e)
  }

  def switchToSelectMode(e: ActionEvent): Unit = {
    editor = editor.switchToSelectMode(e)
  }

  def switchToAddCropMode(e: ActionEvent): Unit = {
    editor = editor.switchToAddCropMode(e)
  }
}

class MainController extends Initializable {
  private var imageTable: ImageTable = new ImageTable
  private var stage: Stage = _
  private var project: Project = _
  private var img: Image = _
  private var editor: Editor = _

  def setStage(stage: Stage) {
    this.stage = stage
    stage.setWidth(1024)
    stage.setHeight(800)
    stage.scene().setOnKeyReleased(new EventHandler[KeyEvent] {
      override def handle(t: KeyEvent) {
        t.getCode() match {
          case KeyCode.DELETE =>
            println("DELETE hit")
            project.deleteAllSelectedFields()
          case _ =>
        }
      }
    })
  }

  @FXML
  private[this] var imageListView: ListView[File] = _

  @FXML
  private[this] var imageCanvas: Canvas = _

  @FXML
  private[this] var cropCheck: CheckBox = _

  @FXML
  private[this] var skewCorrectionCheck: CheckBox = _

  lazy val sfxSkewCorrectionCheck = new SfxCheckBox(skewCorrectionCheck)

  @FXML
  private[this] var addModeButton: ToggleButton = _

  @FXML
  private[this] var selectModeButton: ToggleButton = _

  lazy val sfxImageListView = new SfxListView(imageListView.asInstanceOf[ListView[File]])
  lazy val sfxImageCanvas = new SfxCanvas(imageCanvas)

  def redrawRect(rect: Rectangle2D): Unit = {
    println("redrawRect(" + rect + ")")
    val gc = sfxImageCanvas.graphicsContext2D
    gc.drawImage(
      img,
      rect.minX, rect.minY, rect.width, rect.height,
      rect.minX, rect.minY, rect.width, rect.height
    )
    project.leftCropField.foreach { cf =>
      if (cf.intersects(rect)) {
        cf.draw(gc, project.isLeftCropFieldSelected)
      }
    }
    project.topCropField.foreach { cf =>
      if (cf.intersects(rect)) {
        cf.draw(gc, project.isLeftCropFieldSelected)
      }
    }
    project.rightCropField.foreach { cf =>
      if (cf.intersects(rect)) {
        cf.draw(gc, project.isRightCropFieldSelected)
      }
    }
    project.bottomCropField.foreach { cf =>
      if (cf.intersects(rect)) {
        cf.draw(gc, project.isBottomCropFieldSelected)
      }
    }
    project.absoluteFields.normalFields.foreach { af =>
      if (af.intersects(rect)) {
        af.draw(gc, false)
      }
    }
    project.absoluteFields.selectedFields.foreach { af =>
      if (af.intersects(rect)) {
        af.draw(gc, true)
      }
    }
  }

  def drawWidget(widget: Widget[_], isSelected: Boolean): Unit = {
    widget.draw(sfxImageCanvas.graphicsContext2D, isSelected)
  }

  def drawSelectionRect(rect: Rectangle2D): Unit = {
    println("drawSelectionRect(" + rect + ")")
    val gc = sfxImageCanvas.graphicsContext2D
    gc.setLineWidth(2.0)
    gc.setLineDashes()
    gc.setStroke(Color.BLACK)
    gc.setLineDashes(5.0, 5.0)
    gc.strokeRect(rect.minX, rect.minY, rect.width, rect.height)
  }

  def fieldCreated(field: Field): Unit = {
    println("fieldCreated(" + field + ")")

    field match {
      case af: AbsoluteField =>
        val dlg = new SfxTextInputDialog("")
        dlg.title = "フィールドの名前入力"
        dlg.headerText = "フィールドの名前を入力してください。"
        dlg.showAndWait() match {
          case None =>
            redrawRect(field.drawArea)
          case Some(name) =>
            project.addAbsoluteField(af.withName(name), true)
        }
      case cf: CropField =>
        val Left = "左余白検出"
        val Right = "右余白検出"
        val Top = "上余白検出"
        val Bottom = "下余白検出"
        val dlg = new SfxChoiceDialog(Top, Seq(Top, Left, Right, Bottom))
        dlg.title = "余白位置の選択"
        dlg.headerText = "余白位置を選択してください"
        dlg.showAndWait() match {
          case None =>
            redrawRect(field.drawArea)
          case Some(loc) => (loc match {
            case Left => project.addLeftCropField(cf.toLeft, true)
            case Right => project.addRightCropField(cf.toRight, true)
            case Top => project.addTopCropField(cf.toTop, true)
            case Bottom => project.addBottomCropField(cf.toBottom, true)
          }).foreach { oldCropField =>
            redrawRect(oldCropField.drawArea)
          }
        }
    }
  }

  def selectFields(rect: Rectangle2D, e: MouseEvent): Unit = {
    println("selectFields(" + rect + ", " + e + ")")
    project.selectAbsoluteFields(rect, e)
  }

  @FXML
  def exitMenuClicked(event: ActionEvent) {
    println("exitMenuClicked()")
    Main.terminate()
  }

  @FXML
  def imageOpenMenuClicked(event: ActionEvent) {
    val fc = new FileChooser {
      title = "Select image file"
      extensionFilters.add(new FileChooser.ExtensionFilter("Image Files", "*.png"))
    }
    Option(fc.showOpenMultipleDialog(stage)).foreach { files =>
      imageTable.addFiles(files)
    }
  }

  @FXML
  def imageSelected(e: MouseEvent) {
    val file: File = e.getSource().asInstanceOf[ListView[File]].getSelectionModel().getSelectedItem()
    println("Image selected " + file)
    img = new Image(file.toURI().toString())
    sfxImageCanvas.width = img.width.toDouble
    sfxImageCanvas.height = img.height.toDouble
    val ctx = sfxImageCanvas.graphicsContext2D
    ctx.clearRect(0, 0, sfxImageCanvas.width.toDouble, sfxImageCanvas.height.toDouble)
    ctx.drawImage(img, 0, 0)
    project.redraw()
  }

//  @FXML
//  def skewCorrectionChanged(e: ActionEvent) {
//    project.skewCorrection = project.skewCorrection.withEnabled(skCorChkBox.isSelected())
//  }

  @FXML
  def skewCorrectionDetailClicked(e: ActionEvent) {
    println("skewCorrectionDetail")
    val loader = new FXMLLoader(getClass().getResource("skewCorrectionDialog.fxml"))
    val root: DialogPane = loader.load()
    val ctrl = loader.getController().asInstanceOf[SkewCorrectionDetailController]
    ctrl.model = project.skewCorrection
    val alert = new SfxAlert(AlertType.Confirmation)
    alert.dialogPane = new SfxDialogPane(root)
    alert.title = "傾き補正"
    alert.onCloseRequest = new EventHandler[DialogEvent] {
      override def handle(t: DialogEvent) {
        println("event type = " + t.getEventType)
        println("target = " + t.getTarget)
        println("result = " + t.getTarget.asInstanceOf[Alert].getResult)
//        t.consume()
      }
    }
    alert.showAndWait().map(_.delegate) match {
      case Some(ButtonType.APPLY) =>
        val model = ctrl.model
        println("apply skew correction detail " + model)
        project.skewCorrection = model

      case Some(_) => println("canceled")
      case None => println("bt = none")
    }

//    val scene: Scene = new Scene(root)
//    val myStage = new Stage()
//    myStage.setTitle("傾き補正")
//    myStage.initModality(Modality.ApplicationModal)
//    myStage.initOwner(stage.getOwner())
//    myStage.setScene(scene)
//    myStage.show()
  }

  def prepareFileForCaptureApi(): Path = {
    val file = Files.createTempFile(null, null)
    using(new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file.toFile)))) { zos =>
      val entry = new ZipEntry("config.json")
      zos.putNextEntry(entry)

      zos.write(
        Json.obj(
          "Hello" -> 1,
          "World" -> "Foo"
        ).toString.getBytes("utf-8")
      )
    }

    file
  }

  @FXML
  def applyToImageClicked(e: ActionEvent) {
    println(" hit applyToImage")

    val file: Option[File] = Option(imageListView.getSelectionModel.getSelectedItem)
    println("selected file = " + file)

    file.foreach { f =>
      val fileToSubmit: Path = prepareFileForCaptureApi()

      using(
        new URL("http://localhost:9000/capture").openConnection().asInstanceOf[HttpURLConnection]
      )(
        c => c.disconnect()
      ) { conn =>
        conn.setDoOutput(true)
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Content-Type", "application/zip")
        using(conn.getOutputStream) { os =>
          os.write(Files.readAllBytes(fileToSubmit))
        }

        val statusCode = conn.getResponseCode
        println("status = " + statusCode)
        using(new BufferedReader(new InputStreamReader(conn.getInputStream, "utf-8"))) { br =>
          def print(): Unit = {
            br.readLine() match {
              case null =>
              case line => println(line)
            }
          }
        }
      }
    }
  }

  @FXML
  def resetImageClicked(e: ActionEvent) {
    println("hit resetImage")
  }

  @FXML
  def addModeClicked(e: ActionEvent) {
    println("addModeClicked")
    editor.switchToAddMode(e)
  }

  @FXML
  def selectModeClicked(e: ActionEvent) {
    println("selectModeClicked")
    editor.switchToSelectMode(e)
  }

  @FXML
  def addCropModeClicked(e: ActionEvent) {
    println("addCropModeClicked")
    editor.switchToAddCropMode(e)
  }

  @FXML
  def canvasMouseClicked(e: MouseEvent) {
    println("canvasMouseClicked")
    if (e.getClickCount == 2) {
      println("double clicked")
      project.getSelectedAbsoluteField(e.getX, e.getY) foreach { f =>
        val dlg = new SfxTextInputDialog(f.name)
        dlg.title = "フィールドの名前変更"
        dlg.headerText = "フィールドの名前を入力してください。"
        dlg.showAndWait() foreach { newName =>
          project.renameSelectedAbsoluteField(f, newName)
        }
      }
    }
  }

  @FXML
  def canvasMousePressed(e: MouseEvent) {
    println("canvasMousePressed")
    editor.onMousePressed(e)
  }

  @FXML
  def canvasMouseDragged(e: MouseEvent) {
    println("canvasMouseDragged")
    editor.onMouseDragged(e)
  }

  @FXML
  def canvasMouseReleased(e: MouseEvent) {
    println("canvasMouseReleased")
    editor.onMouseReleased(e)
  }

  @FXML
  def canvasMouseEntered(e: MouseEvent) {
    canvasMouseMoved(e)
  }

  @FXML
  def canvasMouseExited(e: MouseEvent) {
    stage.scene.get().setCursor(Cursor.DEFAULT)
  }

  @FXML
  def canvasMouseMoved(e: MouseEvent) {
    if (selectModeButton.isSelected) {
      project.possibleMouseOperation(e.getX, e.getY) match {
        case CanDoNothing =>
          stage.scene.get().setCursor(Cursor.DEFAULT)

        case CanMove(f) =>
          stage.scene.get().setCursor(Cursor.MOVE)

        case CanNorthResize(f) =>
          stage.scene.get().setCursor(Cursor.N_RESIZE)

        case CanEastResize(f) =>
          stage.scene.get().setCursor(Cursor.E_RESIZE)

        case CanWestResize(f) =>
          stage.scene.get().setCursor(Cursor.W_RESIZE)

        case CanSouthResize(f) =>
          stage.scene.get().setCursor(Cursor.S_RESIZE)

        case CanNorthWestResize(f) =>
          stage.scene.get().setCursor(Cursor.NW_RESIZE)

        case CanNorthEastResize(f) =>
          stage.scene.get().setCursor(Cursor.NE_RESIZE)

        case CanSouthWestResize(f) =>
          stage.scene.get().setCursor(Cursor.SW_RESIZE)

        case CanSouthEastResize(f) =>
          stage.scene.get().setCursor(Cursor.SE_RESIZE)
      }
    }
  }

  override def initialize(url: URL, resourceBundle: ResourceBundle) {
    imageTable.addChangeListener(new ImageTableListener() {
      override def onFilesChanged(before: imm.Set[File], after: imm.Set[File]) {
        sfxImageListView.items = ObservableBuffer(after.toSeq)
      }
    });

    addModeButton.setSelected(true)

    project = ProjectImpl(
      new ProjectContext(
        onNormalAbsoluteFieldAdded = (f: AbsoluteField) => {
          println("*** normal field added " + f)
          f.draw(sfxImageCanvas.graphicsContext2D, false)
        },
        onSelectedAbsoluteFieldAdded = (f: AbsoluteField) => {
          println("*** selected field added " + f)
          f.draw(sfxImageCanvas.graphicsContext2D, true)
        },
        onNormalAbsoluteFieldRemoved = (f: AbsoluteField) => {
          println("*** normal field removed " + f)
          redrawRect(f.drawArea)
        },
        onSelectedAbsoluteFieldRemoved = (f: AbsoluteField) => {
          println("*** selected field removed " + f)
          redrawRect(f.drawArea)
        },
        onCropFieldAdded = (f: CropField) => {
          redrawRect(f.drawArea)
        }
      ),
      new ProjectListener {
        def onSkewCorrectionChanged(skewCorrection: SkewCorrection) {
          println("Skew correction changed " + skewCorrection)
          sfxSkewCorrectionCheck.selected = skewCorrection.enabled
        }
      }
    )

    editor = new Editor(
      project,
      EditorContext(drawWidget, redrawRect, fieldCreated, drawSelectionRect, selectFields)
    )
    editor.initialize()
  }
}
