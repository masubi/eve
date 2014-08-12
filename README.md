Description:

Simple application that listens on a simple directory for file system events and then executes a shell script.

Does simple event deduplication.

ant run

or

ant build
./run.sh

May need to increase your inotify watches via:

echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf
sudo sysctl -p