import argparse
from pathlib import Path
import shutil
import subprocess


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument("--maven", default=shutil.which("mvn"))
	parser.add_argument("--repository", type=Path, default=Path.home() / ".m2/repository")
	parser.add_argument("--offline", action="store_true")
	arguments = parser.parse_args()

	module = Path(__file__).resolve().parents[3]
	maven = str(Path(arguments.maven).resolve())
	command = [maven, "-B", "-ntp", "-f", str(module / "pom.xml"),
		f"-Dmaven.repo.local={arguments.repository.resolve()}", f"-Dquassar.test.maven={maven}",
		"-Dtest=MavenConsumerTest", "test"]
	if arguments.offline: command.extend(["-o", "-Dquassar.test.maven.offline=true"])
	subprocess.run(command, check=True)


if __name__ == "__main__":
	main()
