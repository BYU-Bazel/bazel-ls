import foo.path.to.package.a as pkg_a
import foo.path.to.package.b as pkg_b
import foo.path.to.package.nested.files.c as pkg_c
import foo.path.to.package.nested.files.d as pkg_d

import foo.path.to.folder.a as fa
import foo.path.to.folder.b as fb

def main():
    print("Hello World!")
    print('')
    pkg_a.pkg_a()
    pkg_b.pkg_b()
    pkg_c.pkg_c()
    pkg_d.pkg_d()
    print('')
    fa.folder_a()
    fb.folder_b()

if __name__ == "__main__":
    main()