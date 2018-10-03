# Useless

[![Build Status](https://travis-ci.org/MateuszKubuszok/useless.svg?branch=master)](https://travis-ci.org/MateuszKubuszok/useless)
[![Maven Central](https://img.shields.io/maven-central/v/com.kubuszok/useless-core_2.12.svg)](https://search.maven.org/search?q=g:com.kubuszok%20useless)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Simple, dependency-free library for writing process managers.

## Installation

Add to your `build.sbt`

```scala
libraryDependencies += "com.kubuszok" %% "useless-core" % uselessVersion
```

If needed you might want to [install integration as well](#integrations).

## Motivation

Sometimes you want to compose several services e.g.

```scala
def createUser(userData: UserData): Future[User] = ...
def createUserResourceGroup(userID: User.ID): Future[ResourceGroup] = ...
def addEntitlementsToResourceGroup(
    userID: User.ID,
    resourceGroupID: ResourceGroup.ID
): Future[Unit] = ...

def createAdmin(userData: UserData): Future[User] = for {
  user <- createUser(userData)
  resourceGroup <- createUserResourceGroup(user.id)
  _ <- addEntitlementsToResourceGroup(user.id, resourceGroup.id)
} yield user
```

However, you soon find out that if server crashes, or if there is some
connection issue between (micro)services, this pipeline will fail and
you end up with half-made service call.

Services lie in different contexts, so you cannot use database transaction
to handle it. You might consider rewriting your API and conventions to
CQRS and ES, but you might think, that what you actually want is an ability
to define a simple saga-pattern like process manager. Possibly with a simple
DSL.

It could look something like this:

```scala
val manger = Manager[Future]

private val createAdminV1: UserData => Future[User] = manager("create-admin-v1") {
  ProcessBuilder
    .create[Future, UserData]
    .retryUntilSucceed(createUser)
    .retryUntilSucceed(user => createUserResourceGroup(user).map(user.id -> _.id))
    .retryUntilSucceed(addEntitlementsToResourceGroup.tupled)
}

def createAdmin(userData: UserData): Future[User] = createAdminV1(userData)
```

That's the idea behind *useless* library.

### Name

A friend of mine told me this idea is retarded, as any perfectly written project
would not have such issues, and if you are in imperfect project then it's your
problem. So this project would be useless.

I though that would be a perfect name for the project.

## Goals

 * handle simple scenarios of cross-service transactions in cases where you
   think full implementation of a saga-pattern would be an overkill,
 * supporting transactions between external services you have no control e.g.
   in your backend app, which most of the time doesn't do complex things in
   a sophisticated way,
 * helping projects where transactions between services are more
   of exceptions than a rule, and so changes to whole architecture would be
   hard to explain.

## Non-goals

 * handling all possible cases with a support of all kinds of behaviors. This
   library only wants to support retry or revert for each stage,
 * implementing saga-pattern and providing support and building blocks for
   distributed transactions - if you have microservices communicating with
   events use them to implement saga pattern instead.

## Assumptions/contracts

The way idea (and assumptions) behind useless looks like this:

 * the service is split into stages - a stage is a function from some input `I`
   to the output `F[O]` (you are able to configure whether `F` would be `Future`,
   `Task`, `IO`, etc, basically TTFI),
 * at the beginning and end of each stage you persist the state (input/output)
   to some persistent storage (`Journal`). It will allow restoring calls in case
   JVM crashed etc,
 * we are assuming, that each stage is idempotent - it is something you, have
   to take case of,
 * in order to persist the current state of the service input and output should
   be (de)serializable - here we call it `PersistentArguments`,
 * implementation of `Journal` is also something you need to provide. This
   way it will surely work out with how you persist things in your application,
 * to be able to resume interrupted services, `Manager` has to know about them.
   So it is your responsibility to register all of them before calling
   `manager.resumeInterruptedServices()`. (You don't need to use them all. You
   register some services for a while to make sure they are finished, and then
   remove them while only exposing the latest one),
 * out of the box, there are two strategies:
   * retry until succeed - it has no assumption about reversibleness of each
     stage. If service fail at stage with such recovering strategy , it will
     try to rerun this stage until it succeed,
   * revert - it is available only of all of the previous stages defined a
     revert (rollback) function. On error it will revert _all previous stages_
     to make it look as if transaction never occurred. (Of course revert
     function should also be idempotent),
 * you are able to define your own strategy, that would make choice between
   retry and revert at each stage, but that is experimental and underspecified.

## Usage

You will setup things in following order:

 * creating `manager` that would handle the transactions for you,
 * passing `manager` to there your services are defined, so that they would
   be both: defined and available to you and registered within manager,
 * once services are defined you can run `manager.resumeInterruptedServices()`
   to make it use journal to resume all interrupted services.

### Journal and Manager

At first, define a `journal` and `manager`:

```scala
import useless._

val journal: Journal[Future] = ??? // this doesn't have to be Future of course
val manager: Manager[Future] = Manager[Future](journal)
```

Both of these require an instance of `useless.algebra.MonadError[F, Throwable]`.
Currently, only an instance for `Future` is defined, but there are extra modules
for lifting Cats/Scalaz instances for it (see below). (I didn't use any of them
here to make sure `useless-core` has literally no dependencies).

Now, you can pass `manager` to where you define your services. If you want, you
might use type bounds to do it in a TTFI way.

```scala
implicit val manager: Manager[F] = Manager[Future](journal)
```

```
class AdminServices[F[_]: Manager](...) {

  val createAdminV1: UserData => Future[User] =
    Manager[F].apply("create-admin-v1") {
      // ProcessBuilder definition here
    }
}
```

Once all services are registered, you can resume interrupted ones with:

```scala
manager.resumeInterruptedServices()
```

It is your responsibility, to make sure there are no several instances of your
application, that would call this all at the same time.

(Have I mentioned that, this aims to be simple? And that people call it useless
for a reason?)

### Defining service

For all services, that should be transactional you have to register them using
`manager`:

```scala
val createAdminV1: UserData => Future[User] = manager("create-admin-v1") {
  ProcessBuilder
    .create[Future, UserData]
    .retryUntilSucceed(createUser)
    .retryUntilSucceed(user => createUserResourceGroup(user).map(user.id -> _.id))
    .retryUntilSucceed(addEntitlementsToResourceGroup.tupled)
}
```

Definition starts with a `ProcessBuilder.create[F, A]`. `F` is your IO type
(`Future`, `IO`, `Task`, etc) matching the type of IO you choose for your
Journal and Manager. `A` is a type of the argument passed to the service.

You are starting with a `ReversibleProcessBuilder` creating `A => F[A]`
service for which you will add building blocks that will take you from
`A` to `F[B]`, from `B` to `F[C]` etc (like in monad, except monadic
interface had some troublesome issues, like how to handle service using 2
arguments from 2 previous stages? Now, you have to explicitly pass them
through as a pair, so it's easy to reason, (de)serialize, resume, etc).

```scala
// this is virtually equal to (s: String) => Future.successful(s)
val sth: String => Future[String] = manager("successful") {
  ProcessBuilder.create[Future, String]
}
```

`Reversible` means you can roll it back. You can rollback as long as for
each new stage you pair it with a revert function. You can choose rollback
as a strategy of dealing with error as long as all previous stages were
reversible.

```scala
ProcessBuilder.create[Future, Int]
  // defines revert -> reversible stage
  .retryUntilSuccess[String] { i =>
    Future(i.toString)
  } { s =>
    Future(s.toInt)
  }
  // previous stage is revertible -> can use revert
  // doesn't define revert -> is not revertible itself
  .revertOnFirstFailure[String] { s =>
    Future("value is " + s)
  }
  // previous chain is non-revertible
  // this stage can only be non-revertible
  .retryUntilSuccess[Unit] { s =>
    Future(println(s))
  }
```

In this example we have a definition for `Int => Future[Unit]`, that would:

 * would take an `Int` argument,
 * tried to turn it into `String` as many times as needed in order to succeed,
 * then tried to add a prefix to this String - on failure it would revert
   whole transaction (which here means it would call `String => Future[Int]`),
 * finally, it would try to print the `String`. Again it would try to do it as
   many times as needed to succeed.

At this point there are 3 possible strategies to deal with an error:

 * always retry,
 * revert everything on first error,
 * decide which of 2 using `argument => Retry|Revert` function.

In future, there should be more strategies to chose from.

## Integrations

### Cats

Add to `build.sbt`:

```scala
libraryDependencies += "com.kubuszok" %% "useless-cats" % uselessVersion
```

then import:

```scala
import cats.implicits._
import useless.cats._
```

It will allow you to convert `cats.MonadError` and `cats.Traverse` to
`useless.algebra.MonadError` and `useless.algebra.Sequence`.

### Scalaz

Add to `build.sbt`:

```scala
libraryDependencies += "com.kubuszok" %% "useless-scalaz" % uselessVersion
```

then import:

```scala
import scalaz._
import Scalaz._
import useless.scalaz._
```

It will allow you to convert `scalaz.MonadError` and `scalaz.Traverse` to
`useless.algebra.MonadError` and `useless.algebra.Sequence`.

### Circe

Add to `build.sbt`:

```scala
libraryDependencies += "com.kubuszok" %% "useless-circe" % uselessVersion
```

then import:

```scala
import useless.circe._
```

It will allow you to convert `io.circe.Decoder` and `io.circe.Encoder` to
`useless.PersistentArgument`.
