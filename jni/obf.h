int obf(const char *src, char *key, char *dest, int destlen);
int unobf(const char *src, char *key, char *dest, int destlen);
void strrev(char *p);
void strrev_utf8(char *p);
unsigned int XOR(const char *value, int valuelen, const char *key, char *retval, int bufflen);
void stripe(char *key, int stripe, char *retval, int bufflen);
char *concat(const char *strings[], int len);
void stripchars(char *str, char stripch);
