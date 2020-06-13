#  about this
here is a light weight  service framework apply as maven archtype.

## before to use
1. module
    > app: application module
    >
    > framework: light weight service framework

2. useage
    + net service: based on `Project Reactor` with `Netty`
    + thrid party extened as plugin: just see source under `framework/src/main/java/*.lite.plugin` as example
    + dependcy inject: here just use `SPI` with google auto-service 