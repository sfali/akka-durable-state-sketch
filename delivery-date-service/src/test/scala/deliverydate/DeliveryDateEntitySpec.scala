package deliverydate

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitDurableStateStorePlugin
import com.typesafe.config.{Config, ConfigFactory}
import deliverydate.DeliveryDateEntity.{Reply, UpdateDeliveryDate, UpdateSuccessful}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class DeliveryDateEntitySpec
    extends ScalaTestWithActorTestKit(DeliveryDateEntitySpec.conf)
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

  override implicit val testKitSettings: TestKitSettings = TestKitSettings(system)
  private val replyProbe = TestProbe[Reply]()

  "DeliveryDateEntity" should {
    "update date" in {
      val command = UpdateDeliveryDate(packageId = UUID.randomUUID(), eventId = 3000, replyTo = replyProbe.ref)
      val actor = spawn(DeliveryDateEntity(command.packageId), command.packageId.toString)
      actor ! command
      replyProbe.receiveMessage() shouldBe UpdateSuccessful(command.packageId)
    }
  }
}

object DeliveryDateEntitySpec {
  val conf: Config =
    PersistenceTestKitDurableStateStorePlugin
      .config
      .withFallback(ConfigFactory.parseString("""akka.loglevel= INFO """))
}
