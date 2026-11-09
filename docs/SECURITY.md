# Security model

## Trust boundaries

- Model prompts, completions, tool names, tool arguments, structured output, and retrieved content are untrusted.
- The host application owns identity, tenant scoping, credentials, authorization, consent, authoritative business rules, and server-side validation.
- Tool registration only describes what an agent may request; it is not permission to execute it.
- AgentPolicy must be supplied explicitly. The default policy denies every tool call.
- ToolExecutor fails closed for Deny and RequireApproval. Approval must happen in the host UI/workflow; the host then re-authorizes the operation.

## Required host controls

1. Derive identity and tenant scope from authenticated host state, never model arguments.
2. Validate every argument and re-check current state immediately before side effects.
3. Apply per-user and per-resource authorization on the authoritative service.
4. Require explicit confirmation for sensitive and high-impact operations.
5. Bound tool time, response size, retries, and concurrent operations.
6. Redact credentials and personal data from prompts, logs, metadata, and tool output.
7. Treat tool output and retrieved documents as untrusted instructions/data.
8. Audit consequential actions with the authenticated principal and authoritative result.
9. Make cancellation and partial failure semantics explicit for mutations.
10. Never execute model-generated code or shell commands in the host process.

## SDK guarantees and limits

The core exposes risk labels and a host policy SPI; it does not itself authenticate users, enforce remote permissions, validate a vendor-specific JSON schema, display approval UI, or guarantee that a backend adapter applies policy unless that adapter uses ToolExecutor. Review each adapter and each host tool before production use.
