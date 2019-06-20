package control.models.cpu;

public class DataPathDriver {
    private InstructionMem instructionMem;
    private ControlUnit controlUnit;
    private RegisterFile registerFile;
    private MainMemory mainMemory;
    private ALU alu;

    private int stageIndicator = 0;

    //halt signal
    private Signal HALT = new Signal(0);

    private String pc;
    private String nextPc;
    private String instruction;
    //decoded instruction
    private String oppCode;
    private String regAddr1;
    private String regAddr2;
    private String regWAddr;
    private String immValue;
    private String funcCode;
    private String jumpValue;
    //data that's shared between stages
    private String regReadData1;
    private String regReadData2;
    private String mainMemoryRead;
    private String extendedOffset;

    private String aluResult;

    public DataPathDriver() {
        pc = "0000000000000000";
    }

    public String executeLine(String pc) {
        this.pc = pc;
        stageIF();
        stageID();
        stageEXE();
        stageMEM();
        stageWB();
        return nextPc;
    }

    public int executeStage(int stageNumber) {
        switch (stageNumber) {
            case -1:
                break;
            case 0:
                stageIF();
                stageNumber++;
                break;
            case 1:
                stageID();
                stageNumber++;
                break;
            case 2:
                stageEXE();
                stageNumber++;
                break;
            case 3:
                stageMEM();
                stageNumber++;
                break;
            case 4:
                stageWB();
                stageNumber = 0;
                this.pc = nextPc;
                break;
        }
        return stageNumber;
    }

    private void stageIF() {
        instruction = instructionMem.getInstruction(pc);
        nextPc = Adder.compute(pc, "00000000000000000000000000000001"); //pc + 1
    }

    private void stageID() {
        //decoding the instruction TODO: fix for miniature processor
        oppCode = instruction.substring(4, 8);        // oppCode
        regAddr1 = instruction.substring(8, 12);    // R-rt
        regAddr2 = instruction.substring(12, 16);    // R-rs
        regWAddr = instruction.substring(16, 20);    // R-rd
        immValue = instruction.substring(16, 32);    // I-immediate/offset
        jumpValue = instruction.substring(16, 32);    // J-JumpOffset

        System.out.println(oppCode + " " + regAddr1 + " " + regAddr2 + " " + regWAddr + " " + immValue + " " + jumpValue + " ");

        //setup control unit
        controlUnit.setup(oppCode);

        if(controlUnit.HALT.data == 1)
            HALT.data = 1;

        //setup register file
        String writeRegisterMux = Mux.compute(regAddr2, regWAddr, controlUnit.REGDES);                        //mux TODO: flag = REGDST
        registerFile.setup(regAddr1, regAddr2, writeRegisterMux, controlUnit.REGWRITE);                         //TODO: flag = REGWRITE
        regReadData1 = registerFile.getFirstData();
        regReadData2 = registerFile.getSecondData();

        //extend the offset value
        extendedOffset = Extendor.singExtend(immValue);
    }

    private void stageEXE() {
        //ALU
        String aluInput2 = Mux.compute(regReadData2, extendedOffset, controlUnit.ALUSRC);                    //mux
        aluResult = alu.compute(regReadData1, aluInput2, controlUnit.ALUOP);

        //calculate addresses
//        String jumpAddr = nextPc.substring(0, 3) + Shifter.shift(jumpValue, 2);                   //next pc when instruction is jump (FOR BYTE MEMORY)
        String jumpAddr = jumpValue;
//        String shiftedImm = Shifter.shift(extendedOffset, 2);                                     //shifter result (FOR BYTE MEMORY)
        String shiftedImm = extendedOffset;                                                         //shifter result
        String pcPlusOffset = Adder.compute("0", shiftedImm);                                    //adder result (TODO: relative addr)
        Signal branchSignal = AndUnit.and(alu.getZeroSignal(), controlUnit.BRANCH);
        nextPc = Mux.compute(nextPc, pcPlusOffset, branchSignal);
        nextPc = Mux.compute(nextPc, jumpAddr, controlUnit.JUMP);
    }

    private void stageMEM() {
        mainMemory.setup(aluResult, controlUnit.MEMREAD, controlUnit.MEMWRITE);
        mainMemory.writeData(regReadData2);
        mainMemoryRead = mainMemory.readData();
    }

    private void stageWB() {
        String registerWriteData = Mux.compute(aluResult, mainMemoryRead, controlUnit.MEMTOREG);                //mux TODO: flag = MEMTOREG
        registerFile.write(registerWriteData);
    }

    public InstructionMem getInstructionMem() {
        return instructionMem;
    }

    public void setInstructionMem(InstructionMem instructionMem) {
        this.instructionMem = instructionMem;
    }

    public ControlUnit getControlUnit() {
        return controlUnit;
    }

    public void setControlUnit(ControlUnit controlUnit) {
        this.controlUnit = controlUnit;
    }

    public RegisterFile getRegisterFile() {
        return registerFile;
    }

    public void setRegisterFile(RegisterFile registerFile) {
        this.registerFile = registerFile;
    }

    public MainMemory getMainMemory() {
        return mainMemory;
    }

    public void setMainMemory(MainMemory mainMemory) {
        this.mainMemory = mainMemory;
    }

    public ALU getAlu() {
        return alu;
    }

    public void setAlu(ALU alu) {
        this.alu = alu;
    }

    public Signal getHALT() {
        return HALT;
    }

    public String getPc() {
        return pc;
    }

    public void setPc(String pc) {
        this.pc = pc;
    }

    public void resetDriver() {
        pc = "0000000000000000";
        this.HALT.data = 0;
        this.stageIndicator = 0;
        getMainMemory().resizeMemory(getMainMemory().getMemSize());
        getRegisterFile().resizeMemory(getRegisterFile().getMemSize());
    }
}
