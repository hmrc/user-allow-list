
# user-allow-list

This service is intended to reduce the amount of code service teams need to write, maintain and later remove for managing a list of allowed users (for example, as part of a private beta rollout). It is responsible for storing a list of allowed identifiers that services can check against.

It is used in combination with [user-allow-list-admin-frontend](https://github.com/hmrc/user-allow-list-admin-frontend), which allows you to maintain the list of allowed identifiers for services you are responsible for.

## What it is

- It allows you to use any identifier you provide - the [request model](https://github.com/hmrc/user-allow-list/blob/main/app/models/CheckRequest.scala) expects a string value.
- It stores identifiers securely, using a salted hash.
- It uses mongodb for storing identifiers to ensure multiple instances return the same results.
- It provides an endpoint for services that returns a 200 (present) or 404 (missing) for a single identifier.
- It integrates with [user-allow-list-admin-frontend](https://github.com/hmrc/user-allow-list-admin-frontend) to allow the list of identifiers to be managed on a per-service basis.
- It allows multiple lists for a given service based upon a "feature" parameter.
- It authenticates all requests using internal-auth. Each service can only access its own identifier lists.

## What it is not

- It is not a replacement for other forms of authentication or authorisation. You should still make use of other predicates to ensure a user is allowed to use your service.
- It is not for storing values that need to be retrieved - it only allows you to check if a value is present. You cannot retrieve the original values back.
- It is not for long-term storage. The production TTL is set to 365 days since last update.
- It is not intended for "feature flagging" functionality in your service.
- It does not allow multiple identifiers to be checked in a single request. It is intended for single lookups.
- It does not allow "partial matches". Identifiers must match exactly.
- It cannot manage lists that are shared across multiple services. Each service can only access its own identifier list.

## Integrating with user-allow-list

### 1. Decide on the identifier and feature name

Before starting any implementation you should decide upon a stable identifier you will use to identify users. For example, a national insurance number or other identifier retrieved from auth. _This cannot be a value provided by a user themselves_. 

You should also decide upon a feature name, which is used to identify a specific list.

### 2. Ensuring your service has an internal auth token

If you service doesn't already use internal auth, you will need to raise a request for an internal auth token to be generated for you according to [these instructions](https://confluence.tools.tax.service.gov.uk/display/PLATOPS/Internal+Auth+-+Requesting+Access).

### 3. Add your service to the internal-auth grants for user-allow-list

To allow your service to access identifier lists in user-allow-list, you need to add your service name to the [internal-auth-config](https://github.com/hmrc/internal-auth-config/) entry. There are separate files for this in QA and Production. It will look similar to the below:

```
- grantees:
    service: [ your-service-name-here ]
  permissions:
    - resourceType: user-allow-list
      resourceLocation: '*'
      actions: [ 'READ' ]
```

### 4. Adding a connector to your service

The service exposes a single endpoint to consuming services - `user-allow-list/:feature/check` - where `feature` is a name you can choose, to allow you to manage multiple lists of identifiers. You must first add appropriate tests to ensure this functionality works as you expect ([example](https://github.com/hmrc/claim-child-benefit-frontend/blob/521a961ef277498d8b9da6db5879393466591f6f/it/connectors/UserAllowListConnectorSpec.scala)).

Next, to make a request to this endpoint you will need a connector such as the below:

```scala
@Singleton
class UserAllowListConnector @Inject() (
                                         configuration: Configuration,
                                         httpClient: HttpClientV2
                                       )(implicit ec: ExecutionContext) {

  private val userAllowListService: Service = configuration.get[Service]("microservice.services.user-allow-list")
  private val internalAuthToken: String = configuration.get[String]("internal-auth.token")

  def check(feature: String, value: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient.post(url"$userAllowListService/user-allow-list/$feature/check")
      .setHeader("Authorization" -> internalAuthToken)
      .withBody(Json.toJson(CheckRequest(value)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(true)
          case NOT_FOUND => Future.successful(false)
          case status    => Future.failed(UnexpectedResponseException(status))
        }
      }
}

object UserAllowListConnector {

  final case class UnexpectedResponseException(status: Int) extends Exception with NoStackTrace {
    override def getMessage: String = s"Unexpected status: $status"
  }
}
```

In this example, you will also need to provide configuration that provides the URL for the user-allow-list service and your internal auth token. These will vary depending on whether you are running the service locally or in a deployed environment.

This also requires a request model, such as the below:

```scala
import play.api.libs.json.{Json, OFormat}

final case class CheckRequest(value: String)

object CheckRequest {

  implicit lazy val format: OFormat[CheckRequest] = Json.format
}
```

### 5. Make use of the connector in your service

The example connector above will provide you with a `Future[Boolean]` which can be used in whatever business logic you require. For example, this could be in an `Action` on each controller.

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
