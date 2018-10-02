# Useless

[![Build Status](https://travis-ci.org/MateuszKubuszok/useless.svg?branch=master)](https://travis-ci.org/MateuszKubuszok/useless)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Simple, dependency-free library for writing process managers.

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

## Assumtions

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

At first, define a journal:

```scala
import useless._

val journal: Journal[Future] = ??? // this doesn't have to be Future of course
```

Once you have `journal`, you can create a `manager`:

```scala
val manager: Manager[Future] = Manager[Future](journal)
```

Both of these require an instance of `useless.algebra.MonadError[F, Throwable]`.
Currently, only an instance for `Future` is defined, but eventually there are
going to be integrations with cats and scalaz, which would lift their instances
to useless' ones. (I didn't use any of them here to make sure useless has
literally no dependencies).

Then, for all services, that should be transactional you have to register them
using `manager`:

```scala
val createAdminV1: UserData => Future[User] = manager("create-admin-v1") {
  ProcessBuilder
    .create[Future, UserData]
    .retryUntilSucceed(createUser)
    .retryUntilSucceed(user => createUserResourceGroup(user).map(user.id -> _.id))
    .retryUntilSucceed(addEntitlementsToResourceGroup.tupled)
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
