# Summary:

Design and implement a Service with an API (including data model and the backing implementation) with following functional requirements:

- Caller can send money from their account to an external withdrawal address through an API (See Withdrawal Service Stub)
- Caller can see operation progress

## Requirements:

- Follow template, modify to an extent needed to complete the task
- Assume the API is invoked by multiple systems and services in the same time on behalf of end users.
- Datastore runs in memory for the sake of simplicity
- Runs standalone and doesn't require external pre-installed dependencies like docker

## Goals:

- To demonstrate high quality code and solution design (As if you’re writing code for your current company)
- To demonstrate ability to produce solution without detailed requirements
- To demonstrate the API and functional requirements are working correctly
- You can use any framework or library, but you must keep solution simple and straight to the point (hint: we’re not using Spring)

## Non-goals:

- Not to show ability to use frameworks - The goal of the task is to show fundamentals, not framework knowledge
- No need to implement non-functional pieces, like authentication, monitoring or logging


## Given

- Application skeleton
  - App setup with rest endpoints
  - Test setup
  - Account skeleton
  - Withdrawal service stubs
- Built with Gradle (Kotlin) and Java 21
- Contains minimal rest setup with Javalin and Jackson
- Provides utility libraries: junit 5, assertj, json-unit-assertj, awaitility, rest-assured, mockito (see libs.versions.toml)

## How to send us the solution

- Please upload solution to the Github, Bitbucket or Gitlab. 