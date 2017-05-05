Usage
=====
1. Download `linkerd-1.0.0-exec` from https://github.com/linkerd/linkerd/releases/tag/1.0.0
   to this directory.
2. `> chmod +x linkerd-1.0.0-exec`
3. `> ./linkerd-1.0.0-exec config.yaml`

Linkerd will listening for gRPC requests on port 5000. Requests for mesh://timeService
will be routed to localhost:5050.

1. Start the time service on port 5050.
2. Start the time client pointed to localhost:5000
3. Success!