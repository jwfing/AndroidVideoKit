VERSION_ID=filter 1.0.0.0 64bit
COMPATIBLE=0

TOPDIR=.
INCDIR=$(TOPDIR)/include
LIBDIR=$(TOPDIR)
EXENAME=filter
STATICLIBNAME=libfilter.a
OUTPUT_DIR=$(TOPDIR)

CP=cp -f
RM=rm -f
MKDIR=mkdir -p
RMDIR=rm -Rf
CC=gcc
CXX=g++
LD=ld
AR=ar cr

CFLAGS= -g -Wall -Wformat -Wconversion -Wdeprecated -Wfloat-equal -Werror \
	-Wextra -Wno-unused-parameter -finline-functions -I$(INCDIR) \
	-I/usr/local/include \
	-D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64 -D__VERSION_ID__="\"$(VERSION_ID)\"" \
	-D__COMPATIBLE__="\"$(COMPATIBLE)\""

ifeq ($(INDEBUGGING), 2)
	CFLAGS += -DDEBUG -pg -DPERF_MONITOR
else
	CFLAGS += -DNDEBUG -O
endif

CXXFLAGS=$(CFLAGS)

LDFLAGS= -L$(LIBDIR) -L/usr/local/lib -L/usr/lib 

LDLIBS= -lavformat -lavcodec -lavdevice -lavfilter -lavcore -lavutil -lswscale -lz

ifeq ($(INDEBUGGING), 1)
	LDLIBS += -fprofile-arcs -ftest-coverage
endif

CLEANFILES= *.o *.gc* core.* *.gdb *.out nulllog nulllog.wf

OBJS=filter.o

.PHONY: all
all: $(OUTPUT_DIR)/$(EXENAME)

$(OUTPUT_DIR)/$(EXENAME): $(OBJS)
	if [ ! -d $(OUTPUT_DIR) ]; then $(MKDIR) $(OUTPUT_DIR); fi
	$(CXX) -o $@ $^ $(CXXFLAGS) $(LDFLAGS) $(LDLIBS)

.PHONY: clean
clean: clean_output
	$(RM) $(CLEANFILES)

clean_output:
	$(RM) $(OUTPUT_DIR)/$(EXENAME)
	$(RM) $(OUTPUT_DIR)/$(STATICLIBNAME)

