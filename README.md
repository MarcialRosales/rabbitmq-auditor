# rabbitmq-auditor

Proof of Concept Operations Tool for RabbitMQ. The current version of this Operations tools has two features: A Policy enforcer and a management api that extends the one provided by RabbitMq itself. 

#Policy Enforcement

Goal: Use <a href="https://www.rabbitmq.com/parameters.html#policies"> RabbitMQ's policy</a> to enforce a set of restrictions (such as Maximum Depth) across all vhosts. Users are allowed to create their own policies but the policy enforcer will override them to make them compliant with the overall policy. This overall policy is called "plan" (a.k.a. service plan). 
 
An administrator defines a service plan thru configuration. A plan states what RabbitMq's features and resources are available to vhosts. Features are things like "Mirroring", "Lazy queues", etc. Resources are "maximum messages (or bytes) in a queue" or "maximum time messages can stay in a queue" or "maximum slaves when mirroring is enabled". We can find a more detailed description of a plan in the next sections. Once the plan is defined, this tool will make sure all vhosts are compliant with the plan. For instance, if the plan states that queues cannot have more than 1000 messages, it will make sure that all policies have that restriction. If mirroring is not allowed but a user creates a policy with mirroring, the policy enforcer automatically removes mirroring from the user's policy.     
 

**The principle is simple** 
 - When the tool starts up, it makes sure that all vhosts, except the root, has a policy called "_plan" and that is compliant with the configured plan. The tool makes sure that at least there is a policy for all queues in the vhost. Furthermore, it checks that rest of the policies are also compliant. 
 - The tool detects when the user creates and/or updates a policy for a queue. If that policy is not compliant with the plan, the tool overrides the policy to make it compliant. 
 - The tool detects when a new vhost is created, and it creates the "_plan" policy on that vhost.
 
**When a policy is not compliant?**
 - If we have a max-queue-length = 1000 and the policy has not defined that parameter with that value or the policy has that parameter with a smaller value, the policy is not compliant and the tool will override it.
 - If we have a policy with mirroring turned on (e.g. ha-mode=all) and mirroring is not allowed, the tool will automatically remove the mirroring parameters from the policy.
 - If we have a policy with mirroring turned on with a larger number of slaves to the allowed value, the tool automatically overrides the policy so that it only has the allowed number of slaves.
 
**What does it mean 'override'? Will the policy loose the rest of the parameters, for instance, DLX parameters and others?**
It means override only those parameters defined in the plan and leave unmodified the rest of parameters. In other words, the tool is not replacing but merging/overriding. 
   
       
##Configuration

###RabbitMq settings
We need to configure the location of RabbitMq and the credentials of an administrator user and the url of the management console. Check out application.yml file.
```
spring:
  rabbitmq:
    addresses: localhost:5673
    username: guest
    password: guest
    admin: http://localhost:15673
```

###Service plan
We need to configure the plan's settings via the application.yml. 
```    
plan:
  name: _plan					# Name of the plan's policy
  allow-mirror-queues: true		# allow client to declare policies with mirroring
  max-slaves: 1					# maximum number of slaves allowed
  maxMessageTTL: 60000			# all queues' max ttl
  max-queue-length: 2000		# all queues' max length
  max-queue-length-bytes: 7000  # all queues' max length in bytes
```

If we only wanted to enforce queue length in bytes and no mirroring, we would need this configuration:
```    
plan:
  name: _plan					
  allow-mirror-queues: false	   # do not allow mirroring
  max-queue-length-bytes: 1048576  # maximum size for all queues
```

| Plan Definition Attributes               | Description               |
|-----------------------|---------------------------|
| name                  | name of the RabbitMQ policy the tool creates on every vhost (except the root)
| <a href="https://www.rabbitmq.com/maxlength.html">max-queue-length </a>     | maximum number of ready messages in queue |
| max-queue-length-bytes      | maximum number of ready bytes in queue |
| <a href="https://www.rabbitmq.com/ttl.html">max-message-ttl</a>     | maximum number of milliseconds a ready message can stay in queue |
| allow-mirror-queues    | allow mirror queue feature (true or false)  |
| max-slaves    | maximum number of slaves allowed per mirrored queue  |
| <a href="https://www.rabbitmq.com/ha.html#eager-synchronisation">ha-sync-mode</a>    | allowed ha-sync-mode. If not defined means any mode is allowed |
| <a href="https://www.rabbitmq.com/ha.html#cluster-shutdown">ha-promote-on-shutdown</a>   | allowed ha-promote-on-shutdown. If not defined means any mode is allowed |
| <a href="https://www.rabbitmq.com/ha.html#queue-master-location">queue-master-locator </a>   | allowed queue-master-locator strategy. If not defined means any mode is allowed |


###Automatic vs manual policy enforcement
We can configure the policy enforcer to automatically enforce the plan or manually (i.e. on demand by an administrator).

<b>Automatic Policy Enforcer</b></p>
On this mode, the Policy Enforcer will:
  - Enforce the configured plan across all vhosts when we launch it
  - Enforce the configured plan across new created vhosts while Policy enforcer is running
  - Enforce the configured plan when a policy changes (except for policies on the root vhost) 
  
To enable automatic policy enforcement, edit application.yml and set this property ``plan.enforce-mode: automatic`` or pass the following JVM parameter ``-DPLAN_ENFORCE_MODE=automatic``. 

<b>Manual Policy Enforcer</b></p>
On this mode, the Policy Enforcer will only intervene when the administrator requests it. This means that a user can create a not compliant policy. It also means vhosts are not automatically created without a compliant policy. It is the responsibility of the administrator to enforce them.  

The Policy Enforcer exposes the following REST-api to help detect not compliant vhosts and to enforce the plan across one or all not compliant vhosts. We need to pass the credentials of the administrator user using HTTP Basic Authentication.

* Grant access to vhosts
IMPORTANT: In order for our user to be able to list policies or to create or to modify them first we need to grant him access. We need to do this operation every time we create a new vhost (unless we are using `plan.enforce-mode: automatic` which automatically grants the configured user access to the newly created vhost). If we don't grant access to all vhosts, we will not be able to enforce policies or check which policies have been applied.
 
```
$ curl -X POST -u guest:guest http://localhost:8080/plan/vhosts/grantAccess
HTTP/1.1 200 OK
{
  "vhosts": [
    "test8",
    "test7"
  ],
  "vhostsCount": 2
}
``` 
If we send the request again it returns an empty list of vhosts.
```
HTTP/1.1 200 OK
{
  "vhosts": [],
  "vhostsCount": 0
}
```


* Get a list of not compliant vhosts:

```
$ curl -u guest:guest http://localhost:8080/plan/uncompliantVhosts
HTTP/1.1 200 OK
{
  "vHostCount": 3,
  "vHostsWithoutPlan": [test1],
  "uncompliantVhosts": [test2],
  "vHostsWithoutPlanCount": 1,
  "uncompliantVhostsCount": 1
}
```

* Get the current plan 

```
$ curl -u guest:guest http://localhost:8080/plan
HTTP/1.1 200 OK
{
  "name": "_plan",
  "maxQueueLength": 0,
  "maxQueueLengthBytes": 0,
  "maxMessageTtl": 60000,
  "allowMirrorQueues": false,
  "maxSlaves": 1,
  "haSyncMode": "manual",
  "queueMasterLocator": "min-masters",
  "enforceMode": "automatic"
}
```

* Get the current RabbitMq's policy for the plan

```
$ curl -u guest:guest http://localhost:8080/plan/policy
HTTP/1.1 200 OK
{
  "name": "_plan",
  "definition": {
    "queue-master-locator": "min-masters",
    "message-ttl": 60000
  },
  "pattern": ".*",
  "priority": 0,
  "apply-to": "queues"
}
```

* Delete all the enforced RabbitMq's policies.
It deletes all the ``_plan`` policies across all vhosts except the root. It only makes sense to call this request when we are using ``plan.enforce-mode: manual``. 

```
$ curl -X DELETE -u guest:guest http://localhost:8080/plan
``` 

* Delete the enforced RabbitMq's policy on a given vhost.
It deletes the ``_plan`` policy. It only makes sense to call this request when we are using ``plan.enforce-mode: manual``. 

```
$ curl -X DELETE -u guest:guest http://localhost:8080/plan/test2
``` 

* To enforce the plan on a given vhost. 

```
$ curl -X POST -u guest:guest http://localhost:8080/plan/test2/enforce
``` 

* To enforce the plan across all not compliant vhosts. 

```
$ curl -X POST -u guest:guest http://localhost:8080/plan/enforce
``` 


To enable manual policy enforcement, either make sure we don't have the setting ``policyEnforcer.mode`` or edit application.yml and set  ``plan.enforce-mode: manual`` or pass the following JVM parameter ``-D PLAN_ENFORCE_MODE=manual``. 
       
```    
plan:
  name: _plan					
  allow-mirror-queues: false	   # do not allow mirroring
  max-queue-length-bytes: 1048576  # maximum size for all queues
  enforce-mode: manual             # do not automatically enforce plan 
```

 
### Deploy on PCF
If we deploy this tool on PCF, check out the manifest.yml and see how you can override the plan's settings and/or rabbitmq's configuration.

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
    
    PLAN_ENFORCE_MODE: manual
 ```
 
##How to use this tool
First, build the tool:
```  
mvn install
```  
Second, enable rabbitmq-event-exchange plugin in Rabbit. 
```  
rabbitmq-plugins enable rabbitmq_event_exchange
```  
Third, run the tool. We can either run in PCF or standalone. To run it in PCF, we first amend the manifest.yml file with the location and credentials of RabbitMQ and deploy this application in PCF using the manifest. This is the ideal setup because PCF will make sure this application is always running and enforcing the plan.
```  
cf push
```  
To run the tool in standalone mode:
```  
java -DPLAN_ALLOW_MIRROR_QUEUES=false -DPLAN_MAX_QUEUE_LENGTH=3500 -DSPRING_RABBITMQ_USERNAME=admin -DSPRING_RABBITMQ_PASSWORD=admin -jar target/rabbitmq-auditor-0.0.1-SNAPSHOT.jar
```  
We can change the plan in PCF. Let's say we want to enable mirroring at most across 2 nodes and we want to use manual synchronization:
```  
cf set-env rabbitmq-auditor PLAN_ALLOW_MIRROR_QUEUES true
cf set-env rabbitmq-auditor PLAN_HA_SYNC_MODE manual
cf set-env rabbitmq-auditor PLAN_MAX_SLAVES 2 
cf restage rabbitmq-auditor	
```

#Management API

Goal: Expose a new management api that builds on top of the existing RabbitMQ's api and offers more elaborate views of RabbitMQ resources.

##Resource Usage Report
As an administrator of a multi-tenant RabbitMq cluster, I want to be able to know the distribution of connections and channels per vhost/user and queues per vhost and node. 

```
$ curl -u guest:guest localhost:8080/api/resources/usage
HTTP/1.1 200 OK
{
  "nodes": [
    {
      "name": "node1@localhost",
      "connectionCount": 103,
      "channelCount": 102,
      "queueCount": 4
    },
    {
      "name": "node2@localhost",
      "connectionCount": 0,
      "channelCount": 0,
      "queueCount": 3
    },
    {
      "name": "node3@localhost",
      "connectionCount": 0,
      "channelCount": 0,
      "queueCount": 2
    }
  ],
  "vhosts": [
    {
      "connectionCount": 103,
      "channelCount": 102,
      "queueCount": 9,
      "users": [
        {
          "name": "USER1",
          "connectionCount": 50,
          "channelCount": 50
        },
        {
          "name": "LOG",
          "connectionCount": 10,
          "channelCount": 10
        },
        {
          "name": "USER2",
          "connectionCount": 5,
          "channelCount": 5
        },
        {
          "name": "USER3",
          "connectionCount": 10,
          "channelCount": 10
        },
        {
          "name": "guest",
          "connectionCount": 1,
          "channelCount": 0
        },
        {
          "name": "USER4",
          "connectionCount": 7,
          "channelCount": 7
        },
        {
          "name": "USER5",
          "connectionCount": 10,
          "channelCount": 10
        },
        {
          "name": "USER6",
          "connectionCount": 10,
          "channelCount": 10
        }
      ],
      "name": "/"
    }
  ],
  "connectionCount": 103,
  "channelCount": 102,
  "queueCount": 9
}
```

Next version of this request will allow the administrator to filter out those vhosts and/or user with a number of connections/channels greater than a configurable number. Likewise with the number of queues.
       

 