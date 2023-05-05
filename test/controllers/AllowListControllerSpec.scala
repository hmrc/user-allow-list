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

import models.CheckRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
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
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AllowListControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar with BeforeAndAfterEach with OptionValues with ScalaFutures {

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

  ".check" - {

    val permission = Predicate.Permission(
      resource = Resource(
        ResourceType("user-allow-list"),
        ResourceLocation("check")
      ),
      action = IAAction("READ")
    )

    "must return OK when the requested value is on the allow list for the given feature" in {

      when(mockStubBehaviour.stubAuth(Some(permission), Retrieval.username))
        .thenReturn(Future.successful(Retrieval.Username("test-service")))

      when(mockRepository.check(any(), any(), any())).thenReturn(Future.successful(true))
      val checkRequest = CheckRequest("test-value")

      val request =
        FakeRequest(routes.AllowListController.check("test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders(AUTHORIZATION -> "my-token")

      val result = route(app, request).value

      status(result) mustEqual OK
      verify(mockRepository, times(1)).check("test-service", "test-feature", "test-value")
    }

    "must return NOT_FOUND when the requested value is not on the allow list for the given feature" in {

      when(mockStubBehaviour.stubAuth(Some(permission), Retrieval.username))
        .thenReturn(Future.successful(Retrieval.Username("test-service")))

      when(mockRepository.check(any(), any(), any())).thenReturn(Future.successful(false))
      val checkRequest = CheckRequest("test-value")

      val request =
        FakeRequest(routes.AllowListController.check("test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders(AUTHORIZATION -> "my-token")

      val result = route(app, request).value

      status(result) mustEqual NOT_FOUND
      verify(mockRepository, times(1)).check("test-service", "test-feature", "test-value")
    }
  }

  ".checkAllowList" - {

    val permission = Predicate.Permission(
      resource = Resource(
        ResourceType("user-allow-list"),
        ResourceLocation("test-service/check")
      ),
      action = IAAction("READ")
    )

    "must return OK when the requested value is on the allow list for the given feature" in {

      when(mockStubBehaviour.stubAuth(Some(permission), Retrieval.EmptyRetrieval))
        .thenReturn(Future.unit)

      when(mockRepository.check(any(), any(), any())).thenReturn(Future.successful(true))
      val checkRequest = CheckRequest("test-value")

      val request =
        FakeRequest(routes.AllowListController.checkAllowList("test-service", "test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders(AUTHORIZATION -> "my-token")

      val result = route(app, request).value

      status(result) mustEqual OK
      verify(mockRepository, times(1)).check("test-service", "test-feature", "test-value")
    }

    "must return NOT_FOUND when the requested value is not on the allow list for the given feature" in {

      when(mockStubBehaviour.stubAuth(Some(permission), Retrieval.EmptyRetrieval))
        .thenReturn(Future.unit)

      when(mockRepository.check(any(), any(), any())).thenReturn(Future.successful(false))
      val checkRequest = CheckRequest("test-value")

      val request =
        FakeRequest(routes.AllowListController.checkAllowList("test-service", "test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders(AUTHORIZATION -> "my-token")

      val result = route(app, request).value

      status(result) mustEqual NOT_FOUND
      verify(mockRepository, times(1)).check("test-service", "test-feature", "test-value")
    }

    "must fail when the user is not authorised" in {

      when(mockStubBehaviour.stubAuth(Some(permission), Retrieval.EmptyRetrieval))
        .thenReturn(Future.failed(new RuntimeException()))

      val checkRequest = CheckRequest("test-value")

      val request =
        FakeRequest(routes.AllowListController.checkAllowList("test-service", "test-feature"))
          .withJsonBody(Json.toJson(checkRequest))
          .withHeaders(AUTHORIZATION -> "my-token")

      route(app, request).value.failed.futureValue

      verify(mockRepository, times(0)).check(any(), any(), any())
    }
  }
}
