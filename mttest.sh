echo 'Test Home...'
./build.sh -buildfile mtDemo.xml mt-demo0 >mttest.out 2>mttest.err &
echo 'Load & Test Client123...'
./build.sh -buildfile mtDemo.xml mt-demo1 >>mttest.out 2>>mttest.err &
echo 'Load & Test Client456...'
./build.sh -buildfile mtDemo.xml mt-demo2 >>mttest.out 2>>mttest.err &
echo 'Load & Test Client789...'
./build.sh -buildfile mtDemo.xml mt-demo3 >>mttest.out 2>>mttest.err &
tail -f -n10000 mttest.out
