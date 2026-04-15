# Phase 21 Research: Overloading & Generics Support

## Overloaded Methods
Manual resolution in v2.0 relied on counting arguments and trying to match types by name. This fails for:
- Primitive widening (int -> long).
- Autoboxing (int -> Integer).
- Subtyping (String -> Object).

`JavaSymbolSolver` handles these rules natively. Calling `call.resolve()` will automatically select the most specific matching method according to Java Language Specification.

## Complex Generics
CRE currently records `List<String>` as `java.util.List` and `java.lang.String`.
However, we need to ensure that calls like `service.process(name)` where `service` is `GenericService<String>` and `process` takes `T`, correctly resolve to `GenericService::process(T)` or identify that `T` is `String`.

## Type Inference with `var`
Symbol Solver can calculate the type of `var` declarations, which manual parsing cannot easily do.

## Implementation Verification
I will create a complex fixture `OverloadGenericController` and verify that the graph edges point to the correct method signatures.
