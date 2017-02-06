# wiremock-races

When testing client socket timeouts, `wiremock.verify()` is not reliable in terms of counting number of received calls, looks like some race conditions in the testcase or in wiremock. Is this to be expected with these type of client timeout settings and these type of tests should be verified on the client having sent the request rather than the server having received it? Why does issuing a Thread.sleep() after performing client calls increase the likelihood of calls being registered?
