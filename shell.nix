{ jdk ? "jdk15" }:

let
  config = {
    packageOverrides = p: rec {
      java = p.${jdk};

      sbt = p.sbt.overrideAttrs (
        old: rec {
          version = "1.4.7";

          src = builtins.fetchurl {
            url = "https://github.com/sbt/sbt/releases/download/v${version}/sbt-${version}.tgz";
            sha256 = "1zal5lxbips276v63sp4443nzbzkcv6h13d8nlb1mhm383z5k9y2";
          };

          patchPhase = ''
            echo -java-home ${java} >> conf/sbtopts
          '';
        }
      );
    };
  };

  nixpkgs = fetchTarball {
    name   = "nixos-unstable-2021-02-21";
    url    = "https://github.com/NixOS/nixpkgs/archive/9816b99e71c.tar.gz";
    sha256 = "1dpz36i3vx0c1wmacrki0wsf30if8xq3bnj71g89rsbxyi87lhcm";
  };

  pkgs = import nixpkgs { inherit config; };
in
pkgs.mkShell {
  name = "scala-shell";
  buildInputs = [
    pkgs.coursier
    pkgs.${jdk}
    pkgs.sbt
  ];
}
