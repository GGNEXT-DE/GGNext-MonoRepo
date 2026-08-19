package de.ggnext.transport

open class RpcException(
    message: String,
) : RuntimeException(message)

class RpcTimeoutException(
    route: String,
) : RpcException("RPC timed out: $route")
