# rabbitmq-auditor

Proof of Concept Operations Tool for RabbitMQ. The current version of this operations tools has the following feature.

##Policy Enforcement

The tool allows an administrator user (configured in application.yml) to define a plan for all vhosts in the RabbitMQ Cluster. Through the plan we can enforce that all queues in any vhosts, except the root, have a maximum depth, and/or a maximum depth in bytes, and/or maximum ttl. Furthermore, we can allow or disallow mirror queues and the maximum number of slaves should the client configured a policy with mirroring.

**The principle is simple** 
 - When the tool starts up, it makes sure that all vhosts, except the root, has a policy called "_plan" and that is compliant with the configured plan. The tool makes sure that at least there is a policy for all queues in the vhost. Furthermore, it checks that rest of the policies are also compliant. 
 - The tool detects when the user creates and/or updates a policy for a queue. If that policy is not compliant with the plan, the tool overrides the policy to make it compliant. 
 - The tool detects when a new vhost is created, and it creates the "_plan" policy on that vhost.
 
**When a policy is not compliant?**
 - If we have a max-queue-length = 1000 and the policy has not defined that parameter with that value or the policy has that parameter with a smaller value, the policy is not compliant and the tool will override it.
 - If we have a policy with mirroring turned on (e.g. ha-mode=all) and mirroring is not allowed, the tool will automatically remove the mirroring parameters from the policy.
 - If we have a policy with mirroring turned on with a larger number of slaves to the allowed value, the tool automatically overrides the policy so that it only has the allowed number of slaves.
 
       
##Configuration

1. We need to configure the location of RabbitMq and the credentials of an administrator user and the url of the management console. Check out application.yml file.
```
spring:
  rabbitmq:
    addresses: localhost:5673
    username: guest
    password: guest
    admin: http://localhost:15673
```

2. We need to configure the plan's settings via the application.yml.
```    
plan:
  name: _plan					# Name of the plan's policy
  allow-mirror-queues: true		# allow client to declare policies with mirroring
  max-slaves: 1					# maximum number of slaves allowed
  maxMessageTTL: 60000			# all queues' max ttl
  max-queue-length: 2000		# all queues' max length
  max-queue-length-bytes: 7000  # all queues' max length in bytes
```

3. If we deploy this tool on PCF, check out the manifest.yml and see how you can override the plan's settings and/or rabbitmq's configuration.

```  
 ....
 env:
    SPRING_RABBITMQ_ADDRESSES: localhost:5673
    SPRING_RABBITMQ_USERNAME: admin
    SPRING_RABBITMQ_PASSWORD: admin
    SPRING_RABBITMQ_ADMIN: http://localhost:15673
    
    PLAN_ALLOW_MIRROR_QUEUES: false
    PLAN_MAX_QUEUE_LENGTH: 10000
    PLAN_MAX_QUEUE_LENGTH_BYTES: 100000
    PLAN_MAX_MESSAGE_TTL: 60000
    PLAN_MAX_SLAVES: 1
 ```