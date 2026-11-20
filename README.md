# ShardShop

A shard-powered tool shop for Paper 1.21+ (Java 21). Requires **ShopPlugin** (it provides the shard balances).

## Features
- `/shardshop` opens a chest GUI; admins can add/remove items in-game
- Self-destructing (24h) Shard Pickaxe / Axe / Shovel with multi-break
- Bottom row: Ranks (leaderboard), Shards (balance), Buy History

## Commands
| Command | Description |
|---|---|
| `/shardshop` | Open the shop |
| `/shardshop additem <price> [hours] [pickaxe\|axe\|shovel]` | Add the item in your main hand |
| `/shardshop removeitem <id>` | Remove an item |
| `/shardshop list` | List shop items |
| `/shardshop reload` | Reload config and items |

Permissions: `shardshop.use` (default: everyone), `shardshop.admin` (default: op)

## Build
GitHub: open the **Actions** tab, pick the latest run, and download the `ShardShop` artifact.
Locally: `mvn clean package` -> `target/ShardShop-1.0.0.jar`
