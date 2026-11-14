# Sample M1 model

Create an M1 model in Quassar Studio using the Workflows DSL. Copy its download URL to load this model from Java. [Open Quassar Studio](https://quassar.io/studio) · [Workflow metamodel](sample-m2-model.md)

```tara
Action(name = "Send confirmation") SendConfirmation
Action(name = "Queue for retry") QueueForRetry
Action(name = "Log failure") LogFailure
Action(name = "Reserve stock") ReserveStock
Action(name = "Notify the warehouse") NotifyWarehouse

Workflow OrderFulfillment
	EntryPoint(name = "Order placed") OrderPlaced
	Step(label = "Confirm order", timeoutSeconds = 30, enabled = true, action = SendConfirmation) Confirm
		fallbackActions = QueueForRetry LogFailure
	Step(label = "Reserve stock", timeoutSeconds = 20, enabled = true, action = ReserveStock) Reserve
	Step(label = "Notify warehouse", timeoutSeconds = 10, enabled = false, action = NotifyWarehouse) Notify
```

Each object is a *mogram*. `Workflow` is a type and `OrderFulfillment` is its identifier. Indentation makes `Confirm` a child of the workflow. The value `SendConfirmation` refers to an action by identifier.
