
#ifndef _FILEIO_H_
#define _FILEIO_H_

/* Function prototypes */
uint8 *loadFromZipByName(char *archive, char *filename, int *filesize);
int check_zip(char *filename);
int gzsize(gzFile *gd);

#endif /* _FILEIO_H_ */
