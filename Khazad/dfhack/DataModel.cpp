#include "DFCommon.h"


DfVector DMWindows40d::readVector (uint32_t offset, uint32_t item_size)
{
    /*
        MSVC++ vector is four pointers long
        ptr allocator
        ptr start
        ptr end
        ptr alloc_end
     
        we don't care about alloc_end because we don't try to add stuff
        we also don't care about the allocator thing in front
    */
    uint32_t start = MreadDWord(offset+4);
    uint32_t end = MreadDWord(offset+8);
    uint32_t size = (end - start) /4;
    return DfVector(start,size,item_size);
};


const string DMWindows40d::readSTLString (uint32_t offset)
{
    /*
        MSVC++ string
        ptr allocator
        union
        {
            char[16] start;
            char * start_ptr
        }
        Uint32 length
        Uint32 capacity
    */
    uint32_t start_offset = offset + 4;
    uint32_t length = MreadDWord(offset + 20);
    uint32_t capacity = MreadDWord(offset + 24);
    char * temp = new char[capacity+1];
    
    // read data from inside the string structure
    if(capacity < 16)
    {
        Mread(start_offset, capacity, (uint8_t *)temp);
    }
    else // read data from what the offset + 4 dword points to
    {
        start_offset = MreadDWord(start_offset);// dereference the start offset
        Mread(start_offset, capacity, (uint8_t *)temp);
    }
    
    temp[length] = 0;
    string ret = temp;
    delete temp;
    return ret;
};


DfVector DMLinux40d::readVector (uint32_t offset, uint32_t item_size)
{
    /*
        GNU libstdc++ vector is three pointers long
        ptr start
        ptr end
        ptr alloc_end

        we don't care about alloc_end because we don't try to add stuff
    */
    uint32_t start = MreadDWord(offset);
    uint32_t end = MreadDWord(offset+4);
    uint32_t size = (end - start) /4;
    return DfVector(start,size,item_size);
};


const string DMLinux40d::readSTLString (uint32_t offset)
{
    // GNU std::string is a single pointer (as far as we are concerned)
    offset = MreadDWord(offset);
    return MreadCString(offset);
};