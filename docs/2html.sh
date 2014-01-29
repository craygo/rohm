f=$1
t=`tempfile`
b=`basename $f .md`
o=$b.html


echo -n '{ "text" : "' >$t
cat $f  | tr '\n' '\\' | sed 's/\\/\\n/g' >> $t
echo '", ' >>$t
echo '  "mode": "gfm", "context": "craygo/rohm" }' >>$t

curl -v -X POST \
  --header "Content-Type:application/json" \
  -d @$t \
  -o $o \
https://api.github.com/markdown

rm $t
