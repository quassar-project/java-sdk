# Sample model (M1)

Create an M1 model in Quassar Studio using the Workflows DSL. Copy its download URL to load this model from Java. [Open Quassar Studio](https://quassar.io/studio) · [Sample metamodel (M2)](sample-m2-model.md)

```tara
Action(name = "Send confirmation") SendConfirmation
Action(name = "Queue for retry") QueueForRetry
Action(name = "Log failure") LogFailure
Action(name = "Reserve stock") ReserveStock
Action(name = "Notify the warehouse") NotifyWarehouse

Action(name = "Validate return") ValidateReturn
Action(name = "Issue refund") IssueRefund

Workflow OrderFulfillment
	EntryPoint(name = "Order placed") OrderPlaced
	Step(label = "Confirm order", timeoutSeconds = 30, enabled = true, action = SendConfirmation) Confirm
		fallbackActions = QueueForRetry LogFailure
	Step(label = "Reserve stock", timeoutSeconds = 20, enabled = true, action = ReserveStock) Reserve
	Step(label = "Notify warehouse", timeoutSeconds = 10, enabled = false, action = NotifyWarehouse) Notify

Workflow ReturnProcessing
	EntryPoint(name = "Return requested") ReturnRequested
	Step(label = "Validate return", timeoutSeconds = 20, enabled = true, action = ValidateReturn) Validate
	Step(label = "Issue refund", timeoutSeconds = 45, enabled = true, action = IssueRefund) Refund
		fallbackActions = QueueForRetry LogFailure
```

`ReturnProcessing` validates a return and issues a refund. It shares the retry and logging actions with `OrderFulfillment`.

Each object is a *mogram*. `Workflow` is a type and `OrderFulfillment` is its identifier. Indentation makes `Confirm` a child of the workflow. The value `SendConfirmation` refers to an action by identifier.
