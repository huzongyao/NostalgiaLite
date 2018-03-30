/*
    state.c --
    Save state management.
*/

#include "shared.h"

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

void system_save_state(void *fd)
{
    char *tmp = malloc(57815);
    int dataSize = system_save_state_mem(tmp);
    fwrite(tmp, dataSize, 1, fd);
    free(tmp);
 }

void system_load_state(void *fd)
{
	char *tmp = malloc(57815);
	fread(tmp, 57815, 1, fd);
	system_load_state_mem(tmp);
	free(tmp);
}



int system_save_state_mem(char *bufPtr)
{
	char * id = STATE_HEADER;
	uint16 version = STATE_VERSION;
	char * start = bufPtr;
	// header
    memcpy(bufPtr, id, 4);
    bufPtr+=4;
    memcpy(bufPtr, &version, sizeof(version));
    bufPtr+=sizeof(version);


	/* Save VDP context */
    memcpy(bufPtr, &vdp, sizeof(vdp_t));
    bufPtr+=sizeof(vdp_t);

    /* Save SMS context */
    memcpy(bufPtr, &sms, sizeof(sms_t));
    bufPtr+=sizeof(sms_t);

    memcpy(bufPtr, &cart.fcr[0], 1);
    bufPtr++;
    memcpy(bufPtr, &cart.fcr[1], 1);
    bufPtr++;
    memcpy(bufPtr, &cart.fcr[2], 1);
    bufPtr++;
    memcpy(bufPtr, &cart.fcr[3], 1);
    bufPtr++;
    memcpy(bufPtr, &cart.sram[0], 0x8000);
    bufPtr+=0x8000;
    /* Save Z80 context */
    memcpy(bufPtr, Z80_Context, sizeof(Z80_Regs));
    bufPtr+=sizeof(Z80_Regs);
    memcpy(bufPtr, &after_EI, sizeof(int));
    bufPtr+=sizeof(int);
    /* Save YM2413 context */
    memcpy(bufPtr, FM_GetContextPtr(), FM_GetContextSize());
    bufPtr+=FM_GetContextSize();

    memcpy(bufPtr,SN76489_GetContextPtr(0), SN76489_GetContextSize());
    bufPtr+=SN76489_GetContextSize();

    int size =  (int)((char*)bufPtr - (char*)start);
    return size;
}

void system_load_state_mem(char *data)
{
    int i;
    uint8 *buf;
    char id[4];
    uint16 version;

    /* Initialize everything */
    z80_reset(0);
    z80_set_irq_callback(sms_irq_callback);
    system_reset();
    if(snd.enabled)
        sound_reset();


    data+=4 + 2;

    /* Load VDP context */
    memcpy(&vdp, data, sizeof(vdp_t));
    data+=sizeof(vdp_t);

    /* Load SMS context */
    memcpy(&sms, data, sizeof(sms_t));
    data+=sizeof(sms_t);

    memcpy(&cart.fcr[0], data, 1);
    data++;
    memcpy(&cart.fcr[1], data, 1);
    data++;
    memcpy(&cart.fcr[2], data, 1);
    data++;
    memcpy(&cart.fcr[3], data, 1);
    data++;

    memcpy(&cart.sram[0], data, 0x8000);
    data+= 0x8000;

    /* Load Z80 context */
    memcpy(Z80_Context, data, sizeof(Z80_Regs));
    data+=sizeof(Z80_Regs);
    memcpy(&after_EI, data,sizeof(int));
    data+=sizeof(int);

    /* Load YM2413 context */
    buf = malloc(FM_GetContextSize());
    memcpy(buf, data, FM_GetContextSize());
    data+=FM_GetContextSize();

    FM_SetContext(buf);
    free(buf);

    /* Load SN76489 context */
    buf = malloc(SN76489_GetContextSize());
    memcpy(buf, data, SN76489_GetContextSize());
    data+=SN76489_GetContextSize();

    SN76489_SetContext(0, buf);
    free(buf);

    /* Restore callbacks */
    z80_set_irq_callback(sms_irq_callback);

    for(i = 0x00; i <= 0x2F; i++)
    {
        cpu_readmap[i]  = &cart.rom[(i & 0x1F) << 10];
        cpu_writemap[i] = dummy_write;
    }

    for(i = 0x30; i <= 0x3F; i++)
    {
        cpu_readmap[i] = &sms.wram[(i & 0x07) << 10];
        cpu_writemap[i] = &sms.wram[(i & 0x07) << 10];
    }

    sms_mapper_w(3, cart.fcr[3]);
    sms_mapper_w(2, cart.fcr[2]);
    sms_mapper_w(1, cart.fcr[1]);
    sms_mapper_w(0, cart.fcr[0]);

    /* Force full pattern cache update */
    bg_list_index = 0x200;
    for(i = 0; i < 0x200; i++)
    {
        bg_name_list[i] = i;
        bg_name_dirty[i] = -1;
    }

    /* Restore palette */
    for(i = 0; i < PALETTE_SIZE; i++)
        palette_sync(i, 1);
}




