[![Build Status](https://travis-ci.org/jasongilanfarr/context.svg?branch=master)](https://travis-ci.org/jasongilanfarr/context)

# Context

Library for supporting Context Local Storage (Thread Local Storage that propagates across asynchronous boundaries).
Usage:

In order to propagate the context across asynchronous boundaries, wrappers for ExecutionContext and ExecutorService
are provided, e.g. `ContextPropagatingExecutionContext` and `ContextPropagatingExecutorService` which also propagate
SLF4J's MDC.

## Akka Integration

Akka has its own dispatcher (since actors are more or less lightweight threads). A dispatcher and
scheduler are provided that can be configured the same as the default dispatcher and scheduler
but will propagate MDC and Context for actor messages and scheduled tasks.

To use, include the following in `application.conf`:

```
akka.actor.default-dispatcher.type = "akka.dispatch.ContextAwareDispatcherConfigurator"
akka.scheduler.implementation = "org.thisamericandream.context.akka.ContextAwareScheduler"
```