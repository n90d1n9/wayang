#!/usr/bin/env bash
# Build FAISS C API shared library for Wayang FFM integration.
# Source is placed in ~/.wayang/vendor/faiss
# Output library goes to ~/.wayang/lib/
#
# Usage:
#   ./build-faiss.sh           # Build only if library is missing
#   ./build-faiss.sh --force   # Rebuild even if library exists
set -euo pipefail

VENDOR_DIR="${HOME}/.wayang/vendor"
FAISS_DIR="${VENDOR_DIR}/faiss"
LIB_DIR="${HOME}/.wayang/lib"
BUILD_DIR="${FAISS_DIR}/build"

FORCE_BUILD=false
if [[ "${1:-}" == "--force" ]]; then
    FORCE_BUILD=true
fi

echo "=== Wayang FAISS Native Build ==="

# Check if library already exists
OS="$(uname -s)"
case "${OS}" in
    Darwin) LIB_NAME="libfaiss_c.dylib" ;;
    Linux)  LIB_NAME="libfaiss_c.so" ;;
    *)      echo "Unsupported OS: ${OS}"; exit 1 ;;
esac

if [[ -f "${LIB_DIR}/${LIB_NAME}" && "${FORCE_BUILD}" != "true" ]]; then
    echo "FAISS native library already exists at ${LIB_DIR}/${LIB_NAME}"
    echo "Use --force to rebuild: ./build-faiss.sh --force"
    echo "Or with Maven: mvn compile -Dfaiss.rebuild"
    exit 0
fi

if [[ "${FORCE_BUILD}" == "true" ]]; then
    echo "Force rebuild requested."
fi

# 1. Clone if not present
if [ ! -d "${FAISS_DIR}" ]; then
    echo "Cloning FAISS to ${FAISS_DIR}..."
    mkdir -p "${VENDOR_DIR}"
    git clone --depth 1 https://github.com/facebookresearch/faiss.git "${FAISS_DIR}"
else
    echo "FAISS source found at ${FAISS_DIR}"
fi

# OS-specific CMake arguments
CMAKE_ARGS=(
    "-DFAISS_ENABLE_C_API=ON"
    "-DFAISS_ENABLE_GPU=OFF"
    "-DFAISS_ENABLE_PYTHON=OFF"
    "-DFAISS_ENABLE_EXTRAS=OFF"
    "-DBUILD_TESTING=OFF"
    "-DBUILD_SHARED_LIBS=ON"
    "-DCMAKE_BUILD_TYPE=Release"
)

if [[ "${OS}" == "Darwin" ]]; then
    # Standard macOS AppleClang doesn't support OpenMP natively but FAISS source
    # heavily uses it. We must use Homebrew's LLVM toolchain.
    BREW_PREFIX="$(brew --prefix 2>/dev/null || echo '/opt/homebrew')"
    LLVM_PATH="${BREW_PREFIX}/opt/llvm"
    if [ -d "${LLVM_PATH}" ]; then
        echo "Found Homebrew LLVM at ${LLVM_PATH}. Configuring CMake to use true LLVM clang++."
        # Use Homebrew LLVM's clang/clang++ and point to its OpenMP library
        CMAKE_ARGS+=(
            "-DCMAKE_C_COMPILER=${LLVM_PATH}/bin/clang"
            "-DCMAKE_CXX_COMPILER=${LLVM_PATH}/bin/clang++"
            # Pass LDFLAGS and CPPFLAGS explicitly for OpenMP under LLVM
            "-DCMAKE_EXE_LINKER_FLAGS=-L${LLVM_PATH}/lib"
            "-DCMAKE_SHARED_LINKER_FLAGS=-L${LLVM_PATH}/lib"
            "-DCMAKE_MODULE_LINKER_FLAGS=-L${LLVM_PATH}/lib"
        )
        export LDFLAGS="-L${LLVM_PATH}/lib"
        export CPPFLAGS="-I${LLVM_PATH}/include"
    else
        echo "WARNING: Homebrew LLVM not found. FAISS requires OpenMP to compile on macOS."
        echo "AppleClang does not support OpenMP properly."
        echo "Please install LLVM and libomp: brew install llvm libomp"
        exit 1
    fi
fi

# 2. Build with CMake
echo "Building FAISS C API..."
mkdir -p "${BUILD_DIR}"
cmake -S "${FAISS_DIR}" -B "${BUILD_DIR}" --fresh "${CMAKE_ARGS[@]}"

cmake --build "${BUILD_DIR}" --target faiss_c -j "$(nproc 2>/dev/null || sysctl -n hw.ncpu)"

# 3. Copy to ~/.wayang/lib/
mkdir -p "${LIB_DIR}"
case "${OS}" in
    Darwin)
        cp "${BUILD_DIR}/c_api/libfaiss_c.dylib" "${LIB_DIR}/" 2>/dev/null || \
        cp "${BUILD_DIR}/faiss/libfaiss.dylib" "${LIB_DIR}/" 2>/dev/null || true
        # Also copy the core faiss lib that libfaiss_c depends on
        find "${BUILD_DIR}" -name "libfaiss*.dylib" -exec cp {} "${LIB_DIR}/" \;
        echo "Libraries copied to ${LIB_DIR}/"
        ls -la "${LIB_DIR}"/libfaiss*
        ;;
    Linux)
        find "${BUILD_DIR}" -name "libfaiss*.so*" -exec cp {} "${LIB_DIR}/" \;
        echo "Libraries copied to ${LIB_DIR}/"
        ls -la "${LIB_DIR}"/libfaiss*
        ;;
esac

echo ""
echo "=== FAISS build complete ==="
echo "Library path: ${LIB_DIR}"
echo "Add to JVM: --enable-native-access=ALL-UNNAMED -Djava.library.path=${LIB_DIR}"
