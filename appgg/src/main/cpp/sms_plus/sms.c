/*
    sms.c --
    Sega Master System console emulation.
*/
#include "shared.h"

/* SMS context */
sms_t sms;

uint8 dummy_write[0x400];
uint8 dummy_read[0x400];

void writemem_mapper_none(int offset, int data)
{
    cpu_writemap[offset >> 10][offset & 0x03FF] = data;
}

void writemem_mapper_sega(int offset, int data)
{
    cpu_writemap[offset >> 10][offset & 0x03FF] = data;
    if(offset >= 0xFFFC)
        sms_mapper_w(offset & 3, data);
}

void writemem_mapper_codies(int offset, int data)
{
    switch(offset & 0xC000)
    {
        case 0x0000:
            sms_mapper_w(1, data);
            return;
        case 0x4000:
            sms_mapper_w(2, data);
            return;
        case 0x8000:
            sms_mapper_w(3, data);
            return;
        case 0xC000:
            cpu_writemap[offset >> 10][offset & 0x03FF] = data;
            return;
    }

}

void sms_init(void)
{
    z80_init();

    sms_reset();

    /* Default: open bus */
    data_bus_pullup     = 0x00;
    data_bus_pulldown   = 0x00;

    /* Assign mapper */
    cpu_writemem16 = writemem_mapper_sega;
    if(cart.mapper == MAPPER_CODIES)
        cpu_writemem16 = writemem_mapper_codies;

    /* Force SMS (J) console type if FM sound enabled */
    if(sms.use_fm)
    {
        sms.console = CONSOLE_SMSJ;
        sms.territory = TERRITORY_DOMESTIC;
        sms.display = DISPLAY_NTSC;
    }

    /* Initialize selected console emulation */
    switch(sms.console)
    {
        case CONSOLE_SMS:
            cpu_writeport16 = sms_port_w;
            cpu_readport16 = sms_port_r;
            break;

        case CONSOLE_SMSJ:
            cpu_writeport16 = smsj_port_w;
            cpu_readport16 = smsj_port_r;
            break;
  
        case CONSOLE_SMS2:
            cpu_writeport16 = sms_port_w;
            cpu_readport16 = sms_port_r;
            data_bus_pullup = 0xFF;
            break;

        case CONSOLE_GG:
            cpu_writeport16 = gg_port_w;
            cpu_readport16 = gg_port_r;
            data_bus_pullup = 0xFF;
            break;

        case CONSOLE_GGMS:
            cpu_writeport16 = ggms_port_w;
            cpu_readport16 = ggms_port_r;
            data_bus_pullup = 0xFF;
            break;

        case CONSOLE_GEN:
        case CONSOLE_MD:
            cpu_writeport16 = md_port_w;
            cpu_readport16 = md_port_r;
            break;

        case CONSOLE_GENPBC:
        case CONSOLE_MDPBC:
            cpu_writeport16 = md_port_w;
            cpu_readport16 = md_port_r;
            data_bus_pullup = 0xFF;
            break;
    }
}

void sms_shutdown(void)
{
    /* Nothing to do */
}

void sms_reset(void)
{
    int i;

    
    z80_reset(NULL);
    z80_set_irq_callback(sms_irq_callback);

    /* Clear SMS context */
    memset(dummy_write, 0, sizeof(dummy_write));
    memset(dummy_read,  0, sizeof(dummy_read));
    memset(sms.wram,    0, sizeof(sms.wram));
    memset(cart.sram,    0, sizeof(cart.sram));

    sms.paused      = 0x00;
    sms.save        = 0x00;
    sms.fm_detect   = 0x00;
    sms.memctrl     = 0xAB;
    sms.ioctrl      = 0xFF;

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

    cart.fcr[0] = 0x00;
    cart.fcr[1] = 0x00;
    cart.fcr[2] = 0x01;
    cart.fcr[3] = 0x00;
}


void sms_mapper_w(int address, int data)
{
    int i;

    /* Calculate ROM page index */
    uint8 page = (data % cart.pages);

    /* Save frame control register data */
    cart.fcr[address] = data;

    switch(address)
    {
        case 0:
            if(data & 8)
            {
                uint32 offset = (data & 4) ? 0x4000 : 0x0000;
                sms.save = 1;

                for(i = 0x20; i <= 0x2F; i++)
                {
                    cpu_writemap[i] = cpu_readmap[i]  = &cart.sram[offset + ((i & 0x0F) << 10)];
                }
            }
            else
            {
                for(i = 0x20; i <= 0x2F; i++)
                {          
                    cpu_readmap[i] = &cart.rom[((cart.fcr[3] % cart.pages) << 14) | ((i & 0x0F) << 10)];
                    cpu_writemap[i] = dummy_write;
                }
            }
            break;

        case 1:
            for(i = 0x01; i <= 0x0F; i++)
            {
                cpu_readmap[i] = &cart.rom[(page << 14) | ((i & 0x0F) << 10)];
            }
            break;

        case 2:
            for(i = 0x10; i <= 0x1F; i++)
            {
                cpu_readmap[i] = &cart.rom[(page << 14) | ((i & 0x0F) << 10)];
            }
            break;

        case 3:
            if(!(cart.fcr[0] & 0x08))
            {
                for(i = 0x20; i <= 0x2F; i++)
                {
                    cpu_readmap[i] = &cart.rom[(page << 14) | ((i & 0x0F) << 10)];
                }
            }
            break;
    }
}




int sms_irq_callback(int param)
{
    return 0xFF;
}



