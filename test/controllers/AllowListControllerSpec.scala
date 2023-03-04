package controllers

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType}
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.ExecutionContext.global

class AllowListControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  private val mockStubBehaviour = mock[StubBehaviour]
  private val backendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents(), global)
  private val permission = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType("user-allow-list"),
      resourceLocation = ResourceLocation("check")
    ),
    action = IAAction("READ")
  )

  ".check" - {

    "must return OK when the user is on the allow list for the given feature" - {

      
    }
  }
}
