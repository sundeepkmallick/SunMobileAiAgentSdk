# Tool development

1. Implement AgentTool in the host application or a domain integration.
2. Give it a stable unique name and JSON object parameter schema.
3. Describe constraints concisely.
4. Validate arguments again at execution time. A schema is not authorization.
5. Use the host AgentPolicy to allow or deny invocation, or require human approval.
6. Authenticate and authorize against the real backend; never trust model-supplied user, tenant, price, inventory, or permission claims.
7. Return minimal data. Do not return credentials or unnecessary personal data.
8. Test malformed input, denied access, timeout, cancellation, and side effects.

Prefer read-only tools initially. Reversible, sensitive, and high-impact operations need explicit risk classification and host-side confirmation. Financial, order, booking, inventory, and account mutations must be validated by the authoritative service.
