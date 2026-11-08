# Modules

## Included

| Module | Purpose | Dependencies |
| --- | --- | --- |
| :agent-core | Agent contracts, capabilities, sessions, tool registry, policy SPI | Kotlin/JVM, coroutines, kotlinx.serialization |
| :sample-app | Offline Android sample using a clearly labelled mock backend | Android, :agent-core |

## Planned adapter artifacts

| Module | Responsibility | Notes |
| --- | --- | --- |
| :agent-adk | Shared ADK agent/runner lifecycle and tool bridge | Keep vendor model construction out of core |
| :agent-firebase | Firebase AI Logic model provider/configuration | Host configures Firebase; no credentials in SDK |
| :agent-litertlm | LiteRT-LM engine/model provider | Synchronize load, run, release, replacement |
| :agent-gemini-nano | ML Kit Agent API and AppFunctions integration | Platform constraints differ from ADK |
| :agent-model-management | Optional catalogue, verified download, installation | Add after artifact/license/device policy is agreed |

These are intentionally not empty modules or fake adapters. Vendor calls must be ported and compiled against exact dependencies before claiming backend support.

## Host-application code

BOPIS/pickup instructions, inventory/order/store repositories, substitute-product rules, associate approval, pickup response parsing, fallback policy, and AppFunctions exposing those business operations remain in SunCommerzAssociate.
