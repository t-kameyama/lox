#include "chunk.h"
#include "debug.h"

int main(int argc, const char *argv[]) {
    Chunk chunk;
    initChunk(&chunk);

    int constant = addConstant(&chunk, 1.2);
    writeChunk(&chunk, OP_CONSTANT, 11);
    writeChunk(&chunk, constant, 11);

    int constant2 = addConstant(&chunk, 3.4);
    writeChunk(&chunk, OP_CONSTANT, 22);
    writeChunk(&chunk, constant2, 22);

    writeChunk(&chunk, OP_RETURN, 22);

    disassembleChunk(&chunk, "test chunk");
    freeChunk(&chunk);

    return 0;
}
