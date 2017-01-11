
#ifndef CPUINTRF_H
#define CPUINTRF_H

#include "osd_cpu.h"

#define CPU_16BIT_PORT          0x4000
#define CPU_FLAGS_MASK          0xff00

enum
{
	MAX_REGS = 128,				/* maximum number of register of any CPU */

	/* This value is passed to activecpu_get_reg to retrieve the previous
	 * program counter value, ie. before a CPU emulation started
	 * to fetch opcodes and arguments for the current instrution. */
	REG_PREVIOUSPC = -1,

	/* This value is passed to activecpu_get_reg to retrieve the current
	 * program counter value. */
	REG_PC = -2,

	/* This value is passed to activecpu_get_reg to retrieve the current
	 * stack pointer value. */
	REG_SP = -3,

	/* This value is passed to activecpu_get_reg/activecpu_set_reg, instead of one of
	 * the names from the enum a CPU core defines for it's registers,
	 * to get or set the contents of the memory pointed to by a stack pointer.
	 * You can specify the n'th element on the stack by (REG_SP_CONTENTS-n),
	 * ie. lower negative values. The actual element size (UINT16 or UINT32)
	 * depends on the CPU core. */
	REG_SP_CONTENTS = -4
};

enum
{
	/* line states */
	CLEAR_LINE = 0,				/* clear (a fired, held or pulsed) line */
	ASSERT_LINE,				/* assert an interrupt immediately */
	HOLD_LINE,					/* hold interrupt line until acknowledged */
	PULSE_LINE,					/* pulse interrupt line for one instruction */

	/* internal flags (not for use by drivers!) */
	INTERNAL_CLEAR_LINE = 100 + CLEAR_LINE,
	INTERNAL_ASSERT_LINE = 100 + ASSERT_LINE,

	/* interrupt parameters */
	MAX_IRQ_LINES =	16,			/* maximum number of IRQ lines per CPU */
	IRQ_LINE_NMI = 127			/* IRQ line for NMIs */
};


/* daisy-chain link */
typedef struct {
	void (*reset)(int);             /* reset callback     */
	int  (*interrupt_entry)(int);   /* entry callback     */
	void (*interrupt_reti)(int);    /* reti callback      */
	int irq_param;                  /* callback paramater */
}	Z80_DaisyChain;

#define Z80_MAXDAISY	4		/* maximum of daisy chan device */

#define Z80_INT_REQ     0x01    /* interrupt request mask       */
#define Z80_INT_IEO     0x02    /* interrupt disable mask(IEO)  */

#define Z80_VECTOR(device,state) (((device)<<8)|(state))


#endif	/* CPUINTRF_H */
