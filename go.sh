for i in {1..200};
do
    bin/ab -n 100000 -c 256 -k -L small.txt
done
