/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.AllowListRepository
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client.Retrieval.Username
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Resource, ResourceLocation, ResourceType, Retrieval}
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AllowListAdminControllerSpec
  extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with OptionValues
    with ScalaFutures {

  private val mockRepository = mock[AllowListRepository]

  private val mockStubBehaviour = mock[StubBehaviour]
  private val backendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents(), global)

  override def beforeEach(): Unit = {
    Mockito.reset(mockRepository, mockStubBehaviour)
    super.beforeEach()
  }

  private val app = GuiceApplicationBuilder()
    .overrides(
      bind[AllowListRepository].toInstance(mockRepository),
      bind[BackendAuthComponents].toInstance(backendAuthComponents)
    )
    .build()

  ".set" - {

    "must set items in the repository for an authorised user" in {

      val predicate = Permission(Resource(ResourceType("user-allow-list"), ResourceLocation("test-service")), IAAction("ADMIN"))

      when(mockRepository.set(any(), any(), any())).thenReturn(Future.successful(Done))
      when(mockStubBehaviour.stubAuth(Some(predicate), Retrieval.username)).thenReturn(Future.successful(Username("username")))

      val setRequest = SetRequest(Set("value 1", "value 2"))

      val request =
        FakeRequest(routes.AllowListAdminController.set("test-service", "test-feature"))
          .withJsonBody(Json.toJson(setRequest))
          .withHeaders("Authorization" -> "Token foo")

      val result = route(app, request).value

      status(result) mustEqual OK

      verify(mockRepository, times(1)).set("test-service", "test-feature", Set("value 1", "value 2"))
    }

    "must not set any items for an unauthorised user" in {

      val setRequest = SetRequest(Set("value 1", "value 2"))

      val request =
        FakeRequest(routes.AllowListAdminController.set("test-service", "test-feature"))
          .withJsonBody(Json.toJson(setRequest)) // No authorization token

      route(app, request).value.failed.futureValue
      verify(mockRepository, never()).set(any(), any(), any())
    }
  }

  ".delete" - {

    "must delete items in the repository for an authorised user" in {

      val predicate = Permission(Resource(ResourceType("user-allow-list"), ResourceLocation("test-service")), IAAction("ADMIN"))

      when(mockRepository.remove(any(), any(), any())).thenReturn(Future.successful(Done))
      when(mockStubBehaviour.stubAuth(Some(predicate), Retrieval.username)).thenReturn(Future.successful(Username("username")))

      val deleteRequest = DeleteRequest(Set("value 1", "value 2"))

      val request =
        FakeRequest(routes.AllowListAdminController.delete("test-service", "test-feature"))
          .withJsonBody(Json.toJson(deleteRequest))
          .withHeaders("Authorization" -> "Token foo")

      val result = route(app, request).value

      status(result) mustEqual OK

      verify(mockRepository, times(1)).remove("test-service", "test-feature", Set("value 1", "value 2"))
    }

    "must not remove any items for an unauthorised user" in {

      val deleteRequest = DeleteRequest(Set("value 1", "value 2"))

      val request =
        FakeRequest(routes.AllowListAdminController.delete("test-service", "test-feature"))
          .withJsonBody(Json.toJson(deleteRequest)) // No authorization token

      route(app, request).value.failed.futureValue
      verify(mockRepository, never()).remove(any(), any(), any())
    }
  }

  "check" - {

    "must return OK when the requested value is on the allow list for an authorised user" in {

      val predicate = Permission(Resource(ResourceType("user-allow-list"), ResourceLocation("test-service")), IAAction("ADMIN"))

      when(mockRepository.check(any(), any(), any())).thenReturn(Future.successful(true))
      when(mockStubBehaviour.stubAuth(Some(predicate), Retrieval.username)).thenReturn(Future.successful(Username("username")))

      val checkRequest = CheckRequest("value")

      val request =
        FakeRequest(routes.AllowListAdminController.check("test-service", "test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders("Authorization" -> "Token foo")

      val result = route(app, request).value

      status(result) mustEqual OK

      verify(mockRepository, times(1)).check("test-service", "test-feature", "value")
    }

    "must return NOT_FOUND when the requested value is not on the allow list for an authorised user" in {

      val predicate = Permission(Resource(ResourceType("user-allow-list"), ResourceLocation("test-service")), IAAction("ADMIN"))

      when(mockRepository.check(any(), any(), any())).thenReturn(Future.successful(false))
      when(mockStubBehaviour.stubAuth(Some(predicate), Retrieval.username)).thenReturn(Future.successful(Username("username")))

      val checkRequest = CheckRequest("value")

      val request =
        FakeRequest(routes.AllowListAdminController.check("test-service", "test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders("Authorization" -> "Token foo")

      val result = route(app, request).value

      status(result) mustEqual NOT_FOUND

      verify(mockRepository, times(1)).check("test-service", "test-feature", "value")
    }

    "must not check an item for an unauthorised user" in {

      val checkRequest = CheckRequest("value")

      val request =
        FakeRequest(routes.AllowListAdminController.check("test-service", "test-feature"))
          .withJsonBody(Json.toJson(checkRequest)) // No authorization token

      route(app, request).value.failed.futureValue
      verify(mockRepository, never()).check(any(), any(), any())
    }
  }

  ".count" - {

    "must return OK and a count for an authorised user" in {

      val predicate = Permission(Resource(ResourceType("user-allow-list"), ResourceLocation("test-service")), IAAction("ADMIN"))

      when(mockRepository.count(any(), any())).thenReturn(Future.successful(123))
      when(mockStubBehaviour.stubAuth(Some(predicate), Retrieval.username)).thenReturn(Future.successful(Username("username")))

      val checkRequest = CheckRequest("value")

      val request =
        FakeRequest(routes.AllowListAdminController.count("test-service", "test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders("Authorization" -> "Token foo")

      val result = route(app, request).value
      val expectedResponse = CountResponse(123)

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(expectedResponse)
      verify(mockRepository, times(1)).count("test-service", "test-feature")
    }

    "must not return a count for an unauthorised user" in {

      val request =
        FakeRequest(routes.AllowListAdminController.count("test-service", "test-feature")) // No authorization token

      route(app, request).value.failed.futureValue
      verify(mockRepository, never()).count(any(), any())
    }
  }

  ".summary" - {

    "must return OK and a summary for an authorised user" in {

      val predicate = Permission(Resource(ResourceType("user-allow-list"), ResourceLocation("test-service")), IAAction("ADMIN"))

      val summaries = Seq(
        Summary("feature 1", 1),
        Summary("feature 2", 2)
      )

      when(mockRepository.summary(any())).thenReturn(Future.successful(summaries))
      when(mockStubBehaviour.stubAuth(Some(predicate), Retrieval.username)).thenReturn(Future.successful(Username("username")))

      val checkRequest = CheckRequest("value")

      val request =
        FakeRequest(routes.AllowListAdminController.summary("test-service"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders("Authorization" -> "Token foo")

      val result = route(app, request).value
      val expectedResponse = SummaryResponse(summaries)

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(expectedResponse)
      verify(mockRepository, times(1)).summary("test-service")
    }

    "must not return a summary for an unauthorised user" in {

      val request =
        FakeRequest(routes.AllowListAdminController.summary("test-service")) // No authorization token

      route(app, request).value.failed.futureValue
      verify(mockRepository, never()).summary(any())
    }
  }

  ".clear" - {

    "must return OK and clear the feature for an authorised user" in {

      val predicate = Permission(Resource(ResourceType("user-allow-list"), ResourceLocation("test-service")), IAAction("ADMIN"))

      when(mockRepository.clear(any(), any())).thenReturn(Future.successful(Done))
      when(mockStubBehaviour.stubAuth(Some(predicate), Retrieval.username)).thenReturn(Future.successful(Username("username")))

      val checkRequest = CheckRequest("value")

      val request =
        FakeRequest(routes.AllowListAdminController.clear("test-service", "test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders("Authorization" -> "Token foo")

      val result = route(app, request).value

      status(result) mustEqual OK
      verify(mockRepository, times(1)).clear("test-service", "test-feature")
    }

    "must not return a count for an unauthorised user" in {

      val request =
        FakeRequest(routes.AllowListAdminController.clear("test-service", "test-feature")) // No authorization token

      route(app, request).value.failed.futureValue
      verify(mockRepository, never()).clear(any(), any())
    }
  }
}
