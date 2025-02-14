# Grasscutter
A WIP server emulator for Genshin Impact 2.3-2.6

**Note**: For support please join the [Discord server](https://discord.gg/T5vZU6UyeG).

# Current features
* Logging in
* Spawning monsters via console
* Combat
* Inventory features (recieving items/characters, upgrading items/characters, etc)
* Co-op does work, but movement is kind of buggy and some player ults do not spawn properly
* Friends list
* Gacha system

# Running the server and client

### Prerequisites
* JDK-8u202 ([mirror link since Oracle required an account to download old builds](https://mirrors.huaweicloud.com/java/jdk/8u202-b08/))
* Mongodb (recommended 4.0+)
* Fiddler Classic

### Starting up the server (Assuming you are on Windows)
1. Setup compile environment `gradlew.bat`
2. Compile the server with `gradlew jar`
3. Create a folder named `resources` in your server directory, you will need to copy `BinOutput` and `ExcelBinOutput` folders which you can get from a repo like [https://github.com/Dimbreath/GenshinData](https://github.com/Dimbreath/GenshinData) into your resources folder.
4. Run the server with `java -jar grasscutter.jar`. Make sure mongodb is running as well.

### Connecting with the client
½. Create an account using command below
1. Run Fiddler and turn on `Decrypt https traffic` in setting 
2. Set your hosts file to redirect at least api-account-os.hoyoverse.com and dispatchosglobal.yuanshen.com to your dispatch server ip. Or use Fiddler with script from [https://github.lunatic.moe/fiddlerscript](https://github.lunatic.moe/fiddlerscript) (Recommended for beginners)
3. If you're using Fiddler, change the default port there (Tools -> Options -> Connections) to anything other than 8888, otherwise the server won't boot.
4. yoink

### Server console commands

`account create [username] {playerid}` - Creates an account with the specified username and the in-game uid for that account. The playerid parameter is optional and will be auto generated if not set.

### In-Game commands
There is a dummy user named "Server" in every player's friends list that you can message to use commands. Commands also work in other chat rooms, such as private/team chats.

`!spawn [monster id] [level] [amount]`

`!give [item id] [amount]`

`!drop [item id] [amount]`

`!killall`

`!godmode` - Prevents you from taking damage

`!resetconst` - Resets the constellation level on your current active character, will need to relog after using the command to see any changes.

`!sethp [hp]`

`!clearartifacts` - Deletes all unequipped and unlocked level 0 artifacts, **including yellow rarity ones** from your inventory

### Quick Troubleshooting
* If compiling wasnt successful, please check your JDK installation (must be JDK 8 and JDK's bin PATH variable is correct)
* My client doesn't connect, doesn't login, 4206, etc... - Mostly your fiddler is the issue, make sure it running on another port except 8888
* Startup sequence: Mongodb > The server > Fiddler > Client
* If `4206` error constantly prompt up, try to use [jdk-8u202-b08](https://mirrors.huaweicloud.com/java/jdk/8u202-b08/) instead of other versions of JDK
