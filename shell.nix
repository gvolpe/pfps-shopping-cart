{ jdk ? "graalvm11-ce" }:

let
  config = {
    packageOverrides = p: rec {
      java = p.${jdk};

      sbt = p.sbt.overrideAttrs (
        old: rec {
          patchPhase = ''
            echo -java-home ${java} >> conf/sbtopts
          '';
        }
      );
    };
  };

  nixpkgs = fetchTarball {
    name   = "nixpkgs-unstable-2021-04-23";
    url    = "https://github.com/NixOS/nixpkgs/archive/b2c2551614aa.tar.gz";
    sha256 = "1wf3yy2gaphzmxn5iiyp63cm3wj16niafnimx5qh52vgqjw9fbrq";
  };

  pkgs = import nixpkgs { inherit config; };
in
pkgs.mkShell {
  name = "scala-shell";

  buildInputs = [
    pkgs.${jdk}
    pkgs.sbt
  ];

  shellHook = ''
    set -o allexport; source .env; set +o allexport
  '';
}
