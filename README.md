![Logo](https://www.tebex.io/assets/img/logos/tebex.svg#gh-light-mode-only)
![Static Badge](https://img.shields.io/badge/spigot-1.8--1.20-brightgreen)
![Static Badge](https://img.shields.io/badge/fabric-1.16.5+-brightgreen)
![Static Badge](https://img.shields.io/badge/bungeecord-1.18+-brightgreen)
![Static Badge](https://img.shields.io/badge/waterfall-1.18+-brightgreen)
![Static Badge](https://img.shields.io/badge/velocity-1.16.5+-brightgreen)

## What Is Tebex?
Tebex is a game monetization engine featuring over 120 payment methods, chargeback protection, fraud protection, and 3-day payouts. Tebex allows you to sell items, subscriptions, and more from an in-game customized shop.

Players browse your store, select and purchase their items, and Tebex automatically delivers the items when the player is next online. View more at https://tebex.io

See an interactive Tebex store using one of our free templates at https://example.tebex.io/

## Features
- Simple and Powerful. Create fully customizable themed web stores and start selling in minutes.
- No Hidden Fees. Enjoy flat-rate pricing with no surprises.
- Worldwide Payments Support. Accept over 120+ payment types with Tebex acting as your merchant of record, including PayPal, Paysafecard, Google Pay, and more.
- Chargeback and Fraud Protection. Tebex handles fraud reports and disputes/chargebacks on your behalf while providing 100% insurance.
- Made for Everyone. Whether you’re starting or an established network, Tebex offers a comprehensive set of shop management tools to handle every facet of your game’s economy.

## Installation and Setup
1. Create a free webstore at https://tebex.io/
2. Download the latest version of the plugin from the Releases tab of this repository.
3. Place the downloaded Tebex `.jar` in the `plugins` folder of your relevant platform.
4. Restart your server / reload your plugins
5. Run `tebex.secret your-key-here` as a server admin to connect the server to Tebex.

Your secret key can always be found at: https://creator.tebex.io/game-servers. Click Connect Game Server, and then choose Plugin to view your secret key.

## Usage and Commands
Tebex will automatically fulfill any orders from your webstore every two minutes. These are run as server commands, such as giving items or adding groups, which you can define in your store.

Note: Not all commands are available on all platforms. Proxy servers may have a reduced set of commands. Use `tebex.help` to get the relevant list of commands on any platform.

Below are a list of commands used to manage the plugin:

### Permissions
All commands have a permission node which matches with the exact command name. For example a player must have `tebex.help` as a permission in order to view available
commands.

### User Commands
```
tebex.help                          Shows available commands
tebex.secret <key>                  Sets your store’s secret key
tebex.info                          Shows store information
tebex.checkout <packId> <username>  Creates payment link for a package
```

### Administrator Commands
```
tebex.sendlink <name> <package>	    Sends payment link to player
tebex.report <message>		    Reports a problem to Tebex
tebex.ban <name>		    Bans a user from the webstore
tebex.lookup <name>		    Looks up user transaction info
```

### Debug Commands
```
tebex.debug <true/false>    Enables debug logging
tebex.forcecheck            Force runs all time-based events
tebex.refresh               Reloads store and package info
```

## Resources
Here are some additional resources to help you build your Tebex store.

- ❔ [Frequently Asked Questions](https://docs.tebex.io/creators/faq) - Get answers to common questions fast
- 🧠 [Tebex Academy](https://www.youtube.com/@tebex/videos) - Learn to build a successful Tebex store with video tutorials
- ⚙️ [Technical Support](mailto:support@tebex.io) - Get assistance as a buyer or seller, email us at support@tebex.io
- 🖥️ [Developer Documentation](https://docs.tebex.io/developers/) - Develop custom integrations to suit your needs
- 💬 [Feedback Form](https://wkf.ms/45PQwfE) - Help us build a better product by sharing your feedback

## Our Mission
Founded in 2011, our mission has always been the same: helping creators in the gaming industry create new revenue streams without having to invest the time and effort involved in processing and managing global payments.

Since then, we helped generate over half a billion dollars for our clients, providing them with a full suite of monetization features, handling all taxes, billing, and providing full insurance. Making sure they can focus on what they do best - creating great gaming experiences.