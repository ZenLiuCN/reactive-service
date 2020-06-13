[![](https://jitpack.io/v/ZenLiuCN/reactive-service.svg)](https://jitpack.io/#ZenLiuCN/reactive-service)
#  About
This project is design for build a lightweight reactive framework for Net Service.

# Components
1. Reactor Netty : base network layer
2. Typesafe Config : configuration layer
3. SPI and Singleton pattern: dependency control 

## Features
1. A framework skeleton.

    This can be used just as a base dependency to build lightweight reactive service.
    
2. A maven archetype with framework source. 
    
    This will be more convenient as framework is not in a stable status. you can just use maven archetype to build
project within Service framework sources.

## Why

1. Spring is to heavy as framework for small light services.
2. Other framework like helidon ... with some useful features , but somehow not easy to be enhanced.
3. Just like Reactive and with more functional pattern.
4. Build chassis on top of some good wheels is fun.

## Notes

1. Should not be use from big projects, unless you already known all the consequence.
2. Share your idea or even codes. We just wanna world be better than worse (just like something called internet
 companies does).
3. This repo  licenced  under the Apache License, Version 2.0. What it's dependencies are remain to as `IS` to their
 authors.
4. Plus this may only usable under JDK 1.8 (Java 8  JVM 8 ... or else what you call it).
