package olybot.client

import com.raquo.laminar.api.L.*
import io.laminext.core.BrowserUtils
import org.scalajs.dom
import zio.json.*

class StoredVar[T: JsonCodec](name: String, initial: T) {
  private val storageId = s"[StoredString]$name"
  private val updateBus = new EventBus[T => T]()

  val signal: Signal[T] =
    updateBus.events
      .foldLeft[T](
        Option(dom.window.localStorage.getItem(storageId))
          .flatMap(_.fromJson[T].toOption)
          .getOrElse(initial)
      ) { case (current, update) =>
        val newValue = update(current)
        if (BrowserUtils.storageEnabled) {
          dom.window.localStorage.setItem(storageId, newValue.toJson)
        }
        newValue
      }
  def observer: WriteBus[T => T] = updateBus.writer
  
  def update(f: T => T): Unit = updateBus.writer.onNext(f)
  def set(newValue: T): Unit  = updateBus.writer.onNext(_ => newValue)
}
