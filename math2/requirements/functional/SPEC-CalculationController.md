# Class specification — CalculationController

`CalculationController` implements the two GET endpoints in REG-006, preserves
batch order, selects default precision, rejects oversized batches before
evaluation, and submits the validated ordered batch to one
`CalculationService.calculateBatch` call.
