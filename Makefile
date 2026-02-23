.PHONY: android-client android-check test

android-client:
	mkdir -p android/app/src/main/jniLibs/arm64-v8a
	CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build \
		-o android/app/src/main/jniLibs/arm64-v8a/libphoenixclient.so \
		./cmd/android-client/

android-check:
	@echo "Checking android-client compiles for linux/arm64..."
	@CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o /dev/null ./cmd/android-client/ && \
		echo "android-client: OK" || \
		(echo "android-client: FAILED" && exit 1)

test:
	go test ./...