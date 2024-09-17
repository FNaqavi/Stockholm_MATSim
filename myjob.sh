#!/bin/bash -l
# The -l above is required to get the full environment with modules

# Set the allocation to be charged for this job
# not required if you have set a default allocation
#SBATCH -A NAISS2024-5-78

# The name of the script is myjob
#SBATCH -J myjob

# Request 50GB of memory
#SBATCH --mem=50G

# The partition
#SBATCH -p shared

# The number of cores (tasks) requested
#SBATCH -n 21

# 10 hours wall-clock time will be given to this job
#SBATCH -t 8:00:00

# Load the correct Java module (e.g., Java 11 or 17)
module --ignore_cache load "java/17.0.4"

# Check if the module loaded successfully
module list

# Define the base output directory
BASE_OUTPUT_DIR="./output"

# Create a unique directory for this run within the base output directory
RUN_TIMESTAMP=$(date +'%Y%m%d_%H%M%S')
RUN_OUTPUT_DIR="$BASE_OUTPUT_DIR" #/run_$RUN_TIMESTAMP"

# Pre-create the necessary directory structure for MATSim
mkdir -p "$RUN_OUTPUT_DIR/ITERS"
mkdir -p "$RUN_OUTPUT_DIR/temps"

# Set the classpath to include the current directory and the libs directory
CLASSPATH="./Stockholm_MATSim.jar"

# Define the path to the MATSim configuration file
CONFIG_FILE="./src/main/resources/matsim-config.xml"

# Run the compiled Java program with the specified classpath
srun java -cp $CLASSPATH org.matsim.run.RunMatsim $CONFIG_FILE $RUN_OUTPUT_DIR


# Check if the Java program ran successfully
if [ $? -ne 0 ]; then
  echo "Java program failed"
  exit 1
fi

# Check the output directory
echo "Output is stored in: output"
